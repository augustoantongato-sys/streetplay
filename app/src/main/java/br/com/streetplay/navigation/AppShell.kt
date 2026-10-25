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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.streetplay.core.design.LocalAccent
import br.com.streetplay.core.design.SPHeader
import br.com.streetplay.core.design.SPSidebar
import br.com.streetplay.core.design.SPWallpapers
import br.com.streetplay.features.about.AboutScreen
import br.com.streetplay.features.applications.AppsScreen
import br.com.streetplay.features.entertainment.EntertainmentScreen
import br.com.streetplay.features.favorites.FavoritesScreen
import br.com.streetplay.features.home.DiscoverViewModel
import br.com.streetplay.features.home.HomeScreen
import br.com.streetplay.features.home.LauncherViewModel
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.features.live.LiveScreen
import br.com.streetplay.features.live.LiveViewModel
import br.com.streetplay.features.live.PlayerScreen
import br.com.streetplay.features.settings.SettingsScreen
import br.com.streetplay.features.settings.SettingsViewModel
import br.com.streetplay.features.store.StoreScreen
import kotlinx.coroutines.delay

@Composable
fun AppShell(vm: LauncherViewModel, settingsVm: SettingsViewModel, discoverVm: DiscoverViewModel, liveVm: LiveViewModel) {
    var dest by rememberSaveable { mutableStateOf(Destination.HOME) }
    val apps by vm.apps.collectAsStateWithLifecycle()
    val shortcuts by vm.shortcuts.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val prefs by settingsVm.prefs.collectAsStateWithLifecycle()
    val trending by discoverVm.trending.collectAsStateWithLifecycle()
    val liveSource by liveVm.source.collectAsStateWithLifecycle()
    val liveChannels by liveVm.visible.collectAsStateWithLifecycle()
    val liveStats by liveVm.stats.collectAsStateWithLifecycle()
    val liveCounts by liveVm.counts.collectAsStateWithLifecycle()
    val liveShowAll by liveVm.showAll.collectAsStateWithLifecycle()
    val liveLoading by liveVm.loading.collectAsStateWithLifecycle()
    var playing by remember { mutableStateOf<Pair<List<LiveChannel>, Int>?>(null) }
    val accent = LocalAccent.current

    // BACK volta ao Início; no Início não faz nada (launcher não deve fechar).
    BackHandler { if (dest != Destination.HOME) dest = Destination.HOME }

    val contentFocus = remember { FocusRequester() }
    LaunchedEffect(dest) { if (dest == Destination.LIVE) liveVm.loadIfNeeded() }
    LaunchedEffect(dest, playing == null) { delay(80); runCatching { contentFocus.requestFocus() } }

    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    val wallpaper = SPWallpapers[prefs.wallpaper.coerceIn(SPWallpapers.indices)]

    Box(Modifier.fillMaxSize().background(Brush.linearGradient(wallpaper.colors))) {
        // brilho laranja discreto no topo
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.72f, 0f), radius = size.width * 0.45f))
        }
        Row(Modifier.fillMaxSize()) {
            SPSidebar(
                items = Destination.entries.map { it.toSidebarItem() },
                selectedId = dest.name,
                onSelect = { dest = Destination.valueOf(it) },
            )
            Column(Modifier.weight(1f).fillMaxHeight()) {
                SPHeader(onSettings = { dest = Destination.SETTINGS })
                Box(Modifier.weight(1f).fillMaxWidth().focusRequester(contentFocus).focusGroup()) {
                    when (dest) {
                        Destination.HOME -> HomeScreen(
                            shortcuts = shortcuts,
                            favorites = favorites.mapNotNull { byPkg[it] },
                            recents = recents.mapNotNull { byPkg[it] },
                            trending = trending,
                            onOpen = vm::open, onOpenStoreListing = vm::openStoreListing,
                            onMoreApps = { dest = Destination.APPS },
                            onMoreStreaming = { dest = Destination.ENTERTAINMENT },
                            onLive = { dest = Destination.LIVE },
                        )
                        Destination.APPS -> AppsScreen(apps, favorites, loading, vm::open, vm::toggleFavorite)
                        Destination.ENTERTAINMENT -> EntertainmentScreen(shortcuts, apps, vm::open)
                        Destination.LIVE -> LiveScreen(
                            liveSource, liveChannels, liveLoading, liveStats, liveCounts, liveShowAll, liveVm::toggleShowAll, onSelectSource = liveVm::load,
                            onPlay = { i -> playing = liveChannels to i },
                        )
                        Destination.FAVORITES -> FavoritesScreen(apps, favorites, vm::open, vm::toggleFavorite, vm::moveFavorite)
                        Destination.STORE -> StoreScreen(shortcuts, vm::open, vm::openStoreListing)
                        Destination.SETTINGS -> SettingsScreen(
                            prefs, settingsVm::setWallpaper, settingsVm::setAccent, settingsVm::setAnimations,
                            onManageFavorites = { dest = Destination.FAVORITES },
                        )
                        Destination.ABOUT -> AboutScreen()
                    }
                }
            }
        }
        playing?.let { (list, start) -> PlayerScreen(list, start, onClose = { playing = null }) }
    }
}
