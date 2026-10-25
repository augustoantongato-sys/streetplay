package br.com.streetplay.features.live

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.LiveChannel
import kotlinx.coroutines.delay

private enum class Status { LOADING, PLAYING, ERROR }

/** Player em tela cheia. Cima/Baixo (ou CH+/CH-) trocam de canal; Voltar sai. */
@Composable
fun PlayerScreen(channels: List<LiveChannel>, startIndex: Int, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    var index by remember { mutableIntStateOf(startIndex.coerceIn(channels.indices)) }
    var status by remember { mutableStateOf(Status.LOADING) }
    var overlay by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    val channel = channels[index]
    val focus = remember { FocusRequester() }
    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> status = Status.PLAYING
                    Player.STATE_BUFFERING -> if (status != Status.ERROR) status = Status.LOADING
                }
            }
            override fun onPlayerError(error: PlaybackException) { status = Status.ERROR; tick++ }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }

    LaunchedEffect(index) {
        status = Status.LOADING
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(10_000).setReadTimeoutMs(15_000)
        channel.userAgent?.let { http.setUserAgent(it) }
        channel.referrer?.let { http.setDefaultRequestProperties(mapOf("Referer" to it)) }
        player.setMediaSource(DefaultMediaSourceFactory(http).createMediaSource(MediaItem.fromUri(channel.url)))
        player.prepare()
        player.playWhenReady = true
        tick++
    }
    LaunchedEffect(tick) { overlay = true; delay(4_000); if (status != Status.ERROR) overlay = false }
    LaunchedEffect(Unit) { delay(100); runCatching { focus.requestFocus() } }

    fun zap(delta: Int) { index = (index + delta + channels.size) % channels.size }

    Box(
        Modifier.fillMaxSize().background(Color.Black).focusRequester(focus).focusable().onKeyEvent { ev ->
            if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
            when (ev.key) {
                Key.DirectionUp, Key.ChannelUp -> { zap(-1); true }
                Key.DirectionDown, Key.ChannelDown -> { zap(1); true }
                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { tick++; true }
                else -> false
            }
        },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (overlay || status != Status.PLAYING) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(32.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC000000)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SPRemoteImage(channel.logo, Modifier.size(52.dp), ContentScale.Fit)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    SPText(channel.name, SPType.Title, maxLines = 1)
                    SPText(
                        when (status) {
                            Status.LOADING -> "Carregando…"
                            Status.PLAYING -> "Ao vivo"
                            Status.ERROR -> "Canal indisponível agora. Use ▲ ▼ para trocar."
                        },
                        SPType.Body, color = if (status == Status.ERROR) LocalAccent.current else SPColors.TextSecondary,
                    )
                    SPText("Cima/Baixo: trocar de canal   ·   Voltar: sair", SPType.Caption)
                }
            }
        }
    }
}
