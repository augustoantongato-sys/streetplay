package br.com.streetplay.navigation

import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import br.com.streetplay.core.common.openUrl
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.streetplay.core.design.LocalAccent
import br.com.streetplay.core.design.SPHeader
import br.com.streetplay.core.design.SPSidebar
import br.com.streetplay.core.design.SPActionTile
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
    LaunchedEffect(iptvConnected) { if (dest == Destination.LIVE) liveVm.reload() }
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
                onLibrary = { dest = Destination.LIBRARY },
                onFavorites = { liveVm.selectGroup("fav"); dest = Destination.LIVE },
                onSettings = { dest = Destination.SETTINGS },
            )
            Destination.ADD_APPS -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onLibrary = { dest = Destination.LIBRARY }, onFavorites = { dest = Destination.LIVE }, onSettings = { dest = Destination.SETTINGS })
            Destination.APPS -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onLibrary = { dest = Destination.LIBRARY }, onFavorites = { dest = Destination.LIVE }, onSettings = { dest = Destination.SETTINGS })
            Destination.ENTERTAINMENT -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onLibrary = { dest = Destination.LIBRARY }, onFavorites = { dest = Destination.LIVE }, onSettings = { dest = Destination.SETTINGS })
            Destination.LIBRARY -> IptvLibraryScreen(onPlay = { ch -> playing = listOf(ch) to 0 })
            Destination.SERVICES -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onLibrary = { dest = Destination.LIBRARY }, onFavorites = { dest = Destination.LIVE }, onSettings = { dest = Destination.SETTINGS })
            Destination.LIVE -> LiveScreen(
                groups = liveGroups, filters = liveFilters, channels = liveChannels, status = liveStatus, favorites = liveFavs,
                loading = liveLoading, progress = liveProgress,
                onSelectGroup = liveVm::selectGroup, onQuery = liveVm::setQuery, onToggleGrouped = liveVm::toggleGrouped, onOpenGloboplay = {}, onCheck = liveVm::checkChannels,
                onPlay = { list, ch ->
                    if (ch.external) {
                        if (!context.openUrl(ch.url)) Toast.makeText(context, "Instale o app do YouTube ou da Twitch para abrir este canal.", Toast.LENGTH_LONG).show()
                    } else {
                        val playable = list.filter { !it.external }
                        playing = playable to playable.indexOfFirst { it.id == ch.id }.coerceAtLeast(0)
                    }
                }, onToggleFavorite = liveVm::toggleFavorite,
            )
            Destination.FAVORITES -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onLibrary = { dest = Destination.LIBRARY }, onFavorites = { dest = Destination.LIVE }, onSettings = { dest = Destination.SETTINGS })
            Destination.STORE -> StreetPlayHomeScreen(onLive = { dest = Destination.LIVE }, onLibrary = { dest = Destination.LIBRARY }, onFavorites = { dest = Destination.LIVE }, onSettings = { dest = Destination.SETTINGS })
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
        when {
            !iptvConnected -> Box(Modifier.fillMaxSize().focusRequester(contentFocus).focusGroup()) { screen() }
            dest == Destination.HOME -> Box(Modifier.fillMaxSize().focusRequester(contentFocus).focusGroup()) { screen() }
            lite -> Column(Modifier.fillMaxSize()) {
                LiteTopBar(selected = dest, onSelect = navigate)
                Box(Modifier.weight(1f).fillMaxWidth().focusRequester(contentFocus).focusGroup()) { screen() }
            }
            else -> {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Brush.radialGradient(listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.72f, 0f), radius = size.width * 0.45f))
                }
                Row(Modifier.fillMaxSize()) {
                    SPSidebar(
                        items = Destination.entries.filter { it.inMenu }.map { it.toSidebarItem() },
                        selectedId = dest.name,
                        onSelect = { navigate(Destination.valueOf(it)) },
                    )
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        SPHeader(onSettings = { dest = Destination.SETTINGS })
                        Box(Modifier.weight(1f).fillMaxWidth().focusRequester(contentFocus).focusGroup()) { screen() }
                    }
                }
            }
        }
        playing?.let { (list, start) -> PlayerScreen(list, start, onClose = { playing = null }) }
    }
}


@Composable
private fun StreetPlayHomeScreen(
    onLive: () -> Unit,
    onLibrary: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
) {
    val accent = Color(0xFFFF7417)
    val cards = listOf(
        Triple("CANAIS AO VIVO", "TV", onLive),
        Triple("FILMES", "FILME", onLibrary),
        Triple("SÉRIES", "SÉRIE", onLibrary),
        Triple("FAVORITOS", "♡", onFavorites),
        Triple("CONFIGURAÇÕES", "⚙", onSettings),
    )
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF17100C), Color(0xFF080808), Color(0xFF050505)))
        ).padding(horizontal = 42.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            SPText("▶", SPType.Display, color = accent)
            Spacer(Modifier.width(12.dp))
            SPText("STREET PLAY", SPType.Title, color = Color.White)
            Spacer(Modifier.weight(1f))
            SPText("SUA TV, DO SEU JEITO.", SPType.Caption, color = Color(0xFFB5AAA2))
        }
        Box(
            Modifier.fillMaxWidth().weight(1.05f).background(
                Brush.horizontalGradient(listOf(Color(0xFF26150D), Color(0xFF15100D), Color(0xFF080808))),
                androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
            ).padding(horizontal = 38.dp, vertical = 26.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Brush.radialGradient(listOf(Color(0x99FF7417), Color.Transparent), center = Offset(size.width * .78f, size.height * .45f), radius = size.height * .9f), radius = size.height * .9f, center = Offset(size.width * .78f, size.height * .45f))
                drawRect(Brush.horizontalGradient(listOf(Color(0xCC080808), Color.Transparent)))
            }
            Column(Modifier.align(androidx.compose.ui.Alignment.CenterStart).widthIn(max = 590.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SPText("IPTV  •  STREET PLAY", SPType.Label, color = accent)
                SPText("SEU ENTRETENIMENTO", SPType.Display, color = Color.White)
                SPText("EM UM SÓ LUGAR", SPType.Display, color = accent)
                SPText("Canais, filmes e séries, tudo o que você ama, em uma experiência feita para a sua TV.", SPType.Body, color = Color(0xFFE0D8D2), maxLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SPText("＋ CANAIS", SPType.Caption, color = Color.White)
                    SPText("✦ FILMES E SÉRIES", SPType.Caption, color = Color.White)
                    SPText("◉ ALTA QUALIDADE", SPType.Caption, color = Color.White)
                }
            }
        }
        Row(Modifier.fillMaxWidth().weight(.95f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            cards.forEachIndexed { index, card ->
                SPFocusIndicator(
                    onClick = card.third,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(25.dp),
                    container = if (index == 0) Brush.verticalGradient(listOf(Color(0xFFFF8A24), Color(0xFFB83A00))) else Brush.verticalGradient(listOf(Color(0xFF25201D), Color(0xFF0E0D0C))),
                    focusedContainer = Brush.verticalGradient(listOf(Color(0xFFFF9A36), Color(0xFFC64A08))),
                    focusScale = 1.035f,
                ) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        SPText(card.second, TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Bold, color = if (index == 0) Color.White else accent))
                        Spacer(Modifier.height(18.dp))
                        SPText(card.first, TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White), maxLines = 2)
                        Spacer(Modifier.height(12.dp))
                        SPText("→", TextStyle(fontSize = 24.sp, color = Color.White))
                    }
                }
            }
        }
        SPText("O catálogo e a disponibilidade dependem do seu provedor IPTV.", SPType.Caption, color = Color(0xFF8C817A))
    }
}
