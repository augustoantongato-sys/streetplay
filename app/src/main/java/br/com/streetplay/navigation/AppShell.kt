package br.com.streetplay.navigation

import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import br.com.streetplay.core.common.openUrl
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.streetplay.core.design.LocalAccent
import br.com.streetplay.core.design.SPHeader
import br.com.streetplay.core.design.SPSidebar
import br.com.streetplay.core.design.SPActionTile
import br.com.streetplay.core.design.SPFocusIndicator
import br.com.streetplay.core.design.SPText
import br.com.streetplay.core.design.SPType
import br.com.streetplay.domain.AppMode
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.features.about.AboutScreen
import br.com.streetplay.features.applications.AppsScreen
import br.com.streetplay.features.entertainment.EntertainmentScreen
import br.com.streetplay.features.entertainment.IptvLibraryScreen
import br.com.streetplay.features.entertainment.StreamingServicesScreen
import br.com.streetplay.features.favorites.FavoritesScreen
import br.com.streetplay.features.home.AddAppsScreen
import br.com.streetplay.features.home.DiscoverViewModel
import br.com.streetplay.features.home.HomeScreen
import br.com.streetplay.features.home.LauncherViewModel
import br.com.streetplay.features.home.LiteHomeScreen
import br.com.streetplay.features.home.LiteTopBar
import br.com.streetplay.features.live.LiveScreen
import br.com.streetplay.features.live.LiveViewModel
import br.com.streetplay.features.live.PlayerScreen
import br.com.streetplay.features.settings.SettingsScreen
import br.com.streetplay.features.settings.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppShell(vm: LauncherViewModel, settingsVm: SettingsViewModel, discoverVm: DiscoverViewModel, liveVm: LiveViewModel) {
    var dest by rememberSaveable { mutableStateOf(Destination.HOME) }
    val apps by vm.apps.collectAsStateWithLifecycle()
    val shortcuts by vm.shortcuts.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val homeEntries by vm.homeEntries.collectAsStateWithLifecycle()
    val homePins by vm.homePins.collectAsStateWithLifecycle()
    val prefs by settingsVm.prefs.collectAsStateWithLifecycle()
    val iptvConnected by settingsVm.iptvConnected.collectAsStateWithLifecycle()
    val iptvMessage by settingsVm.iptvMessage.collectAsStateWithLifecycle()
    val trending by discoverVm.trending.collectAsStateWithLifecycle()
    val liveGroups by liveVm.groups.collectAsStateWithLifecycle()
    val liveFilters by liveVm.filters.collectAsStateWithLifecycle()
    val liveChannels by liveVm.visible.collectAsStateWithLifecycle()
    val liveFavs by liveVm.favorites.collectAsStateWithLifecycle()
    val liveLoading by liveVm.loading.collectAsStateWithLifecycle()
    val liveProgress by liveVm.progress.collectAsStateWithLifecycle()
    val liveStatus by liveVm.status.collectAsStateWithLifecycle()
    var playing by remember { mutableStateOf<Pair<List<LiveChannel>, Int>?>(null) }
    val accent = LocalAccent.current
    val context = LocalContext.current
    val lite = prefs.mode == AppMode.LITE

    // BACK volta ao Início; no Início não faz nada (launcher não deve fechar).
    BackHandler { if (dest != Destination.HOME) dest = Destination.HOME }

    val contentFocus = remember { FocusRequester() }
    LaunchedEffect(dest) { if (dest == Destination.LIVE) liveVm.loadIfNeeded() }
    LaunchedEffect(iptvConnected) {
        if (iptvConnected) {
            val app = context.applicationContext as br.com.streetplay.StreetPlayApp
            val account = app.container.xtreamAccountStore.load()
            if (account != null) {
                launch {
                    // Aquece o cache em segundo plano sem bloquear a abertura da TV.
                    coroutineScope {
                        listOf(
                            async { runCatching { app.container.xtreamApi.liveChannels(account) } },
                            async { runCatching { app.container.xtreamApi.movies(account) } },
                            async { runCatching { app.container.xtreamApi.series(account) } },
                        ).forEach { it.await() }
                    }
                }
            }
        }
        if (dest == Destination.LIVE) liveVm.reload()
    }
    LaunchedEffect(dest, playing == null, lite) { delay(80); runCatching { contentFocus.requestFocus() } }

    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    // "Loja" abre direto a Google Play Store; as demais opções trocam de tela.
    val navigate: (Destination) -> Unit = { d -> if (d == Destination.STORE) vm.openPlayStore() else dest = d }

    val screen: @Composable () -> Unit = {
        if (!iptvConnected) {
            SettingsScreen(
                prefs = prefs, onMode = settingsVm::setMode, onAccent = settingsVm::setAccent, onAnimations = settingsVm::setAnimations,
                onManageFavorites = {}, onAbout = {},
                iptvConnected = iptvConnected, iptvMessage = iptvMessage,
                onSaveIptv = settingsVm::saveIptv, onDisconnectIptv = settingsVm::disconnectIptv,
                forceIptvLogin = true,
            )
        } else when (dest) {
            Destination.HOME -> StreetPlayHomeScreen(
                onLive = { dest = Destination.LIVE },
                onMovies = { dest = Destination.MOVIES },
                onSeries = { dest = Destination.SERIES },
                onFavorites = { liveVm.selectGroup("fav"); dest = Destination.LIVE },
            )
            Destination.ADD_APPS -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onMovies = { dest = Destination.MOVIES }, onSeries = { dest = Destination.SERIES }, onFavorites = { dest = Destination.LIVE })
            Destination.APPS -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onMovies = { dest = Destination.MOVIES }, onSeries = { dest = Destination.SERIES }, onFavorites = { dest = Destination.LIVE })
            Destination.ENTERTAINMENT -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onMovies = { dest = Destination.MOVIES }, onSeries = { dest = Destination.SERIES }, onFavorites = { dest = Destination.LIVE })
            Destination.LIBRARY, Destination.MOVIES, Destination.SERIES -> IptvLibraryScreen(
                onPlay = { ch -> playing = listOf(ch) to 0 },
                initialSeries = dest == Destination.SERIES,
            )
            Destination.SERVICES -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onMovies = { dest = Destination.MOVIES }, onSeries = { dest = Destination.SERIES }, onFavorites = { dest = Destination.LIVE })
            Destination.LIVE -> LiveScreen(
                groups = liveGroups, filters = liveFilters, channels = liveChannels, status = liveStatus, favorites = liveFavs,
                loading = liveLoading, progress = liveProgress,
                onSelectGroup = liveVm::selectGroup, onQuery = liveVm::setQuery, onToggleGrouped = liveVm::toggleGrouped, onOpenGloboplay = {}, onCheck = {},
                onPlay = { list, ch ->
                    if (ch.external) {
                        if (!context.openUrl(ch.url)) Toast.makeText(context, "Instale o app do YouTube ou da Twitch para abrir este canal.", Toast.LENGTH_LONG).show()
                    } else {
                        val playable = list.filter { !it.external }
                        playing = playable to playable.indexOfFirst { it.id == ch.id }.coerceAtLeast(0)
                    }
                }, onToggleFavorite = liveVm::toggleFavorite,
            )
            Destination.FAVORITES -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onMovies = { dest = Destination.MOVIES }, onSeries = { dest = Destination.SERIES }, onFavorites = { dest = Destination.LIVE })
            Destination.STORE -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onMovies = { dest = Destination.MOVIES }, onSeries = { dest = Destination.SERIES }, onFavorites = { dest = Destination.LIVE })
            Destination.SETTINGS -> SettingsScreen(
                prefs = prefs, onMode = settingsVm::setMode, onAccent = settingsVm::setAccent, onAnimations = settingsVm::setAnimations,
                onManageFavorites = { dest = Destination.FAVORITES }, onAbout = { dest = Destination.ABOUT },
                iptvConnected = iptvConnected, iptvMessage = iptvMessage,
                onSaveIptv = settingsVm::saveIptv, onDisconnectIptv = settingsVm::disconnectIptv,
            )
            Destination.ABOUT -> AboutScreen()
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF080706))) {
        Box(Modifier.fillMaxSize().focusRequester(contentFocus).focusGroup()) { screen() }
        playing?.let { (list, start) -> PlayerScreen(list, start, onClose = { playing = null }) }
    }
}



@Composable
private fun StreetPlayHomeScreen(
    onLive: () -> Unit,
    onMovies: () -> Unit,
    onSeries: () -> Unit,
    onFavorites: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF0B0C10))) {
        val mobile = maxWidth < 600.dp
        val accent = Color(0xFFFF6600)
        val cards = listOf(
            Triple("CANAIS AO VIVO", "TV", onLive),
            Triple("FILMES", "FILME", onMovies),
            Triple("SÉRIES", "SÉRIE", onSeries),
            Triple("FAVORITOS", "♡", onFavorites),
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (mobile) 16.dp else 42.dp)
                .padding(top = if (mobile) 18.dp else 28.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(if (mobile) 18.dp else 22.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(Modifier.size(if (mobile) 42.dp else 52.dp).background(
                    Brush.linearGradient(listOf(Color(0xFFFF7A18), Color(0xFFE94B00))), RoundedCornerShape(14.dp)
                ), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    SPText("▶", TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White))
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    SPText("STREET PLAY", TextStyle(fontSize = if (mobile) 19.sp else 25.sp, fontWeight = FontWeight.Black, color = Color.White))
                    SPText("IPTV PREMIUM", TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accent))
                }
                Spacer(Modifier.weight(1f))
                if (!mobile) SPText("SEU ENTRETENIMENTO", SPType.Caption, color = Color(0xFFAAA6A2))
            }
            Box(
                Modifier.fillMaxWidth().height(if (mobile) 250.dp else 300.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF21130D), Color(0xFF17110E), Color(0xFF090A0D))), RoundedCornerShape(if (mobile) 20.dp else 28.dp))
                    .padding(if (mobile) 20.dp else 36.dp)
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        Brush.radialGradient(listOf(Color(0x77FF6600), Color.Transparent), center = Offset(size.width * .88f, size.height * .5f), radius = size.height * .85f),
                        radius = size.height * .85f, center = Offset(size.width * .88f, size.height * .5f)
                    )
                }
                Column(
                    Modifier.align(if (mobile) androidx.compose.ui.Alignment.BottomStart else androidx.compose.ui.Alignment.CenterStart).widthIn(max = if (mobile) 330.dp else 650.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SPText("STREET PLAY  •  IPTV PREMIUM", SPType.Label, color = accent)
                    SPText("SEU ENTRETENIMENTO", TextStyle(fontSize = if (mobile) 22.sp else 34.sp, fontWeight = FontWeight.Black, color = Color.White))
                    SPText("EM UM SÓ LUGAR", TextStyle(fontSize = if (mobile) 27.sp else 46.sp, fontWeight = FontWeight.Black, color = accent))
                    SPText("Filmes, séries e canais ao vivo em uma experiência feita para você.", TextStyle(fontSize = if (mobile) 12.sp else 15.sp, color = Color(0xFFE0D8D2)), maxLines = 2)
                    SPText("● AO VIVO     ✦ FILMES     ▣ SÉRIES", SPType.Caption, color = Color.White)
                }
                if (!mobile) SPText("▶", TextStyle(fontSize = 112.sp, fontWeight = FontWeight.Black, color = accent.copy(alpha = .85f)), Modifier.align(androidx.compose.ui.Alignment.CenterEnd).padding(end = 46.dp))
            }
            SPText("EXPLORE O STREET PLAY", TextStyle(fontSize = if (mobile) 15.sp else 19.sp, fontWeight = FontWeight.Bold, color = Color.White))
            if (mobile) {
                cards.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { card ->
                            SPFocusIndicator(
                                onClick = card.third, modifier = Modifier.weight(1f).height(116.dp),
                                shape = RoundedCornerShape(18.dp), container = Color(0xFF1A1C23),
                                focusedContainer = Color(0xFFE85A0A), focusScale = 1.025f,
                            ) {
                                Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
                                    SPText(card.second, TextStyle(fontSize = 27.sp, fontWeight = FontWeight.Black, color = accent))
                                    Spacer(Modifier.height(8.dp))
                                    SPText(card.first, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White))
                                }
                            }
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    cards.forEach { card ->
                        SPFocusIndicator(
                            onClick = card.third, modifier = Modifier.weight(1f).fillMaxHeight(),
                            shape = RoundedCornerShape(22.dp), container = Color(0xFF1A1C23),
                            focusedContainer = Color(0xFFE85A0A), focusScale = 1.035f,
                        ) {
                            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                SPText(card.second, TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Black, color = accent))
                                Spacer(Modifier.height(12.dp))
                                SPText(card.first, TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
                            }
                        }
                    }
                }
            }
            SPText("O catálogo e a disponibilidade dependem do seu provedor IPTV.", SPType.Caption, color = Color(0xFF8C817A))
        }
    }
}
