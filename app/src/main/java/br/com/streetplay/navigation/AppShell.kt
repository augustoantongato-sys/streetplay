package br.com.streetplay.navigation

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
            Destination.HOME ->
                if (lite) LiteHomeScreen(
                    entries = homeEntries, favorites = favorites.mapNotNull { byPkg[it] }, recents = recents.mapNotNull { byPkg[it] },
                    trending = trending, onOpen = vm::open, onOpenStoreListing = vm::openStoreListing, onLive = { dest = Destination.LIVE },
                    onLibrary = { dest = Destination.LIBRARY }, onServices = { dest = Destination.SERVICES },
                    onAddApps = { dest = Destination.ADD_APPS }, onRemovePin = vm::togglePin, onMovePin = vm::movePin,
                ) else HomeScreen(
                    shortcuts = shortcuts, entries = homeEntries,
                    favorites = favorites.mapNotNull { byPkg[it] }, recents = recents.mapNotNull { byPkg[it] },
                    trending = trending, onOpen = vm::open, onOpenStoreListing = vm::openStoreListing,
                    onMoreApps = { dest = Destination.APPS }, onMoreStreaming = { dest = Destination.SERVICES },
                    onLive = { dest = Destination.LIVE }, onLibrary = { dest = Destination.LIBRARY }, onServices = { dest = Destination.SERVICES },
                    onAddApps = { dest = Destination.ADD_APPS }, onRemovePin = vm::togglePin, onMovePin = vm::movePin,
                )
            Destination.ADD_APPS -> AddAppsScreen(shortcuts, apps, homePins, vm::togglePin)
            Destination.APPS -> AppsScreen(apps, favorites, loading, vm::open, vm::toggleFavorite)
            Destination.ENTERTAINMENT -> EntertainmentScreen(shortcuts, apps, vm::open)
            Destination.LIBRARY -> IptvLibraryScreen(onPlay = { ch -> playing = listOf(ch) to 0 })
            Destination.SERVICES -> StreamingServicesScreen()
            Destination.LIVE -> LiveScreen(
                groups = liveGroups, filters = liveFilters, channels = liveChannels, status = liveStatus, favorites = liveFavs,
                loading = liveLoading, progress = liveProgress,
                onSelectGroup = liveVm::selectGroup, onQuery = liveVm::setQuery, onToggleGrouped = liveVm::toggleGrouped, onOpenGloboplay = vm::openGloboplay, onCheck = liveVm::checkChannels,
                onPlay = { list, ch ->
                    if (ch.external) {
                        if (!context.openUrl(ch.url)) Toast.makeText(context, "Instale o app do YouTube ou da Twitch para abrir este canal.", Toast.LENGTH_LONG).show()
                    } else {
                        val playable = list.filter { !it.external }
                        playing = playable to playable.indexOfFirst { it.id == ch.id }.coerceAtLeast(0)
                    }
                }, onToggleFavorite = liveVm::toggleFavorite,
            )
            Destination.FAVORITES -> FavoritesScreen(apps, favorites, vm::open, vm::toggleFavorite, vm::moveFavorite)
            Destination.STORE -> Unit
            Destination.SETTINGS -> SettingsScreen(
                prefs = prefs, onMode = settingsVm::setMode, onAccent = settingsVm::setAccent, onAnimations = settingsVm::setAnimations,
                onManageFavorites = { dest = Destination.FAVORITES }, onAbout = { dest = Destination.ABOUT },
                iptvConnected = iptvConnected, iptvMessage = iptvMessage,
                onSaveIptv = settingsVm::saveIptv, onDisconnectIptv = settingsVm::disconnectIptv,
            )
            Destination.ABOUT -> AboutScreen()
        }
    }

    Box(
        Modifier.fillMaxSize().then(
            if (lite) Modifier.background(Color.Black)
            else Modifier.background(Brush.linearGradient(listOf(Color(0xFF0F0F0F), Color(0xFF080808))))
        ),
    ) {
        if (lite) {
            Column(Modifier.fillMaxSize()) {
                LiteTopBar(selected = dest, onSelect = navigate)
                Box(Modifier.weight(1f).fillMaxWidth().focusRequester(contentFocus).focusGroup()) { screen() }
            }
        } else {
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
        playing?.let { (list, start) -> PlayerScreen(list, start, onClose = { playing = null }) }
    }
}
