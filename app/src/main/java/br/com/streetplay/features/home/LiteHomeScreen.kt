package br.com.streetplay.features.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.streetplay.core.common.LocalIconLoader
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.HomeEntry
import br.com.streetplay.domain.InstalledApp
import br.com.streetplay.domain.ShortcutState
import br.com.streetplay.domain.TrendingItem
import br.com.streetplay.navigation.Destination
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Soft = Color(0xD9FFFFFF)

/** Barra superior do modo Lite: nome, abas centralizadas e hora. */
@Composable
fun LiteTopBar(selected: Destination, onSelect: (Destination) -> Unit, modifier: Modifier = Modifier) {
    val now by produceState(Date()) { while (true) { value = Date(); delay(5_000) } }
    val time = remember(now) { SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(now) }
    val tabs = listOf(
        Destination.HOME to "Início", Destination.APPS to "Apps", Destination.LIVE to "TV ao vivo",
        Destination.FAVORITES to "Favoritos", Destination.STORE to "Loja", Destination.SETTINGS to "Ajustes",
    )
    Row(modifier.fillMaxWidth().height(84.dp).padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(200.dp)) { SPText("Street Play", TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)) }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { (dest, label) ->
                val sel = dest == selected
                SPFocusIndicator(
                    onClick = { onSelect(dest) }, shape = RoundedCornerShape(50),
                    container = if (sel) Color.White else Color.Transparent,
                    focusedContainer = if (sel) Color.White else Color(0x33FFFFFF), focusScale = 1.06f,
                ) {
                    Box(Modifier.padding(horizontal = 18.dp, vertical = 9.dp)) {
                        SPText(label, TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (sel) Color.Black else Color.White), maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.width(200.dp), contentAlignment = Alignment.CenterEnd) {
            SPText(time, TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White))
        }
    }
}

@Composable
fun LiteHomeScreen(
    entries: List<HomeEntry>,
    favorites: List<InstalledApp>,
    recents: List<InstalledApp>,
    trending: List<TrendingItem>,
    onOpen: (String) -> Unit,
    onOpenStoreListing: (String) -> Unit,
    onLive: () -> Unit,
    onAddApps: () -> Unit,
    onRemovePin: (String) -> Unit,
    onMovePin: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var missing by remember { mutableStateOf<HomeEntry?>(null) }
    var menuEntry by remember { mutableStateOf<HomeEntry?>(null) }
    var detail by remember { mutableStateOf<TrendingItem?>(null) }
    val first = remember { FocusRequester() }
    LaunchedEffect(entries.isNotEmpty()) { if (entries.isNotEmpty()) { delay(150); runCatching { first.requestFocus() } } }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 56.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
                itemsIndexed(entries, key = { _, e -> e.id }) { i, e ->
                    LiteAppTile(
                        label = e.label, pkg = e.packageName, installed = e.installed,
                        onClick = { val pkg = e.packageName; if (e.installed && pkg != null) onOpen(pkg) else missing = e },
                        onLongClick = { menuEntry = e },
                        modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                    )
                }
                item { LiteIconTile("Adicionar", SPIconKind.PLUS, onAddApps) }
                item { LiteIconTile("Canais", SPIconKind.TV, onLive) }
            }
        }
        if (trending.isNotEmpty()) item {
            LiteSection("Em alta") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                    items(trending, key = { it.type + it.id }) { t ->
                        SPFocusIndicator(
                            onClick = { detail = t }, modifier = Modifier.size(124.dp, 184.dp),
                            shape = RoundedCornerShape(16.dp), container = LiteColors.Tile, focusedContainer = LiteColors.TileFocused, focusScale = 1.08f,
                        ) { SPRemoteImage(t.posterUrl, Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) }
                    }
                }
            }
        }
        if (favorites.isNotEmpty()) item { LiteAppsRow("Favoritos", favorites, onOpen) }
        if (recents.isNotEmpty()) item { LiteAppsRow("Recentes", recents, onOpen) }
    }

    missing?.let { e ->
        SPDialog("${e.label} não está instalado", onDismiss = { missing = null }) {
            e.storePackage?.let { st -> SPButton("Procurar na loja", { onOpenStoreListing(st); missing = null }) }
            SPButton("Remover da tela inicial", { onRemovePin(e.id); missing = null })
            SPButton("Fechar", { missing = null })
        }
    }
    menuEntry?.let { e ->
        val idx = entries.indexOfFirst { it.id == e.id }
        HomeEntryDialog(
            entry = e, canMoveBefore = idx > 0, canMoveAfter = idx in 0 until entries.lastIndex,
            onOpen = { val pkg = e.packageName; if (e.installed && pkg != null) onOpen(pkg) else e.storePackage?.let(onOpenStoreListing) },
            onRemove = { onRemovePin(e.id) }, onMove = { onMovePin(e.id, it) }, onDismiss = { menuEntry = null },
        )
    }
    detail?.let { TrendingDetailsDialog(it) { detail = null } }
}

@Composable
private fun LiteSection(title: String, content: @Composable () -> Unit) {
    Column {
        SPText(title, TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Soft))
        content()
    }
}

@Composable
private fun LiteAppsRow(title: String, apps: List<InstalledApp>, onOpen: (String) -> Unit) {
    LiteSection(title) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            items(apps, key = { it.packageName }) { a -> LiteAppTile(a.label, a.packageName, true, { onOpen(a.packageName) }, size = 96.dp) }
        }
    }
}

/** Ícone grande, só com a imagem. O nome aparece apenas quando o item está em foco. */
@Composable
fun LiteAppTile(label: String, pkg: String?, installed: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 116.dp, onLongClick: (() -> Unit)? = null) {
    val loader = LocalIconLoader.current
    val icon by produceState<ImageBitmap?>(null, pkg, installed) { value = if (pkg != null && installed) loader(pkg) else null }
    var focused by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SPFocusIndicator(
            onClick = onClick, onLongClick = onLongClick, modifier = modifier.size(size).onFocusChanged { focused = it.hasFocus },
            shape = RoundedCornerShape(size * 0.23f), container = LiteColors.Tile, focusedContainer = LiteColors.TileFocused, focusScale = 1.1f,
        ) {
            Box(Modifier.fillMaxSize().alpha(if (installed) 1f else 0.4f), contentAlignment = Alignment.Center) {
                val b = icon
                if (b != null) Image(bitmap = b, contentDescription = null, modifier = Modifier.size(size * 0.56f))
                else SPText(label.take(1).uppercase(), SPType.Display, color = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
        SPText(
            if (installed) label else "Não instalado",
            TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White),
            Modifier.alpha(if (focused) 1f else 0f), maxLines = 1,
        )
    }
}

@Composable
private fun LiteIconTile(label: String, icon: SPIconKind, onClick: () -> Unit, size: Dp = 116.dp) {
    var focused by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SPFocusIndicator(
            onClick = onClick, modifier = Modifier.size(size).onFocusChanged { focused = it.hasFocus },
            shape = RoundedCornerShape(size * 0.23f), container = LiteColors.Tile, focusedContainer = LiteColors.TileFocused, focusScale = 1.1f,
        ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { SPIcon(icon, size = 40.dp) } }
        Spacer(Modifier.height(8.dp))
        SPText(label, TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White), Modifier.alpha(if (focused) 1f else 0f), maxLines = 1)
    }
}
