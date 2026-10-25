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
import androidx.compose.ui.unit.sp
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
    var typed by remember { mutableStateOf("") }
    var source by remember { mutableIntStateOf(0) }
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
            override fun onPlayerError(error: PlaybackException) {
                // tenta a próxima fonte do mesmo canal antes de desistir
                if (source < channels[index].options.lastIndex) source++ else { status = Status.ERROR; tick++ }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }

    LaunchedEffect(index, source) {
        status = Status.LOADING
        val opt = channel.options[source.coerceIn(channel.options.indices)]
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(10_000).setReadTimeoutMs(15_000)
        opt.userAgent?.let { http.setUserAgent(it) }
        opt.referrer?.let { http.setDefaultRequestProperties(mapOf("Referer" to it)) }
        player.setMediaSource(DefaultMediaSourceFactory(http).createMediaSource(MediaItem.fromUri(opt.url)))
        player.prepare()
        player.playWhenReady = true
        tick++
    }
    LaunchedEffect(tick) { overlay = true; delay(4_000); if (status != Status.ERROR) overlay = false }
    LaunchedEffect(Unit) { delay(100); runCatching { focus.requestFocus() } }
    // Digitar o número do canal (como no controle de uma TV)
    LaunchedEffect(typed) {
        if (typed.isNotEmpty()) {
            delay(1_500)
            typed.toIntOrNull()?.let { n -> if (n in 1..channels.size) { source = 0; index = n - 1 } }
            typed = ""
        }
    }

    fun zap(delta: Int) { source = 0; index = (index + delta + channels.size) % channels.size }

    Box(
        Modifier.fillMaxSize().background(Color.Black).focusRequester(focus).focusable().onKeyEvent { ev ->
            if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
            digitOf(ev.key)?.let { if (typed.length < 4) typed += it; return@onKeyEvent true }
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
        if (typed.isNotEmpty()) {
            SPText(typed, SPType.Display.copy(fontSize = 56.sp), Modifier.align(Alignment.TopEnd).padding(36.dp))
        }
        if (overlay || status != Status.PLAYING) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(32.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC000000)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SPRemoteImage(channel.logo, Modifier.size(52.dp), ContentScale.Fit)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    SPText("${index + 1}  ·  ${channel.name}", SPType.Title, maxLines = 1)
                    SPText(
                        when (status) {
                            Status.LOADING -> if (source > 0) "Tentando outra fonte (${source + 1}/${channel.options.size})…" else "Carregando…"
                            Status.PLAYING -> "Ao vivo"
                            Status.ERROR -> "Canal indisponível agora (testei ${channel.options.size} fonte(s)). Use Cima/Baixo para trocar."
                        },
                        SPType.Body, color = if (status == Status.ERROR) LocalAccent.current else SPColors.TextSecondary,
                    )
                    SPText("Cima/Baixo ou número: trocar de canal   ·   Voltar: sair", SPType.Caption)
                }
            }
        }
    }
}

private fun digitOf(key: Key): Int? = when (key) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}
