package br.com.streetplay.features.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.streetplay.R
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.HomeEntry
import br.com.streetplay.domain.InstalledApp
import br.com.streetplay.domain.ShortcutState
import br.com.streetplay.domain.TrendingItem
import kotlinx.coroutines.delay

private val Gap = 11.dp
private val RowSectionTitle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)

@Composable
fun HomeScreen(
    shortcuts: List<ShortcutState>,
    entries: List<HomeEntry>,
    favorites: List<InstalledApp>,
    recents: List<InstalledApp>,
    trending: List<TrendingItem>,
    onOpen: (String) -> Unit,
    onOpenStoreListing: (String) -> Unit,
    onMoreApps: () -> Unit,
    onMoreStreaming: () -> Unit,
    onLive: () -> Unit,
    onLibrary: () -> Unit,
    onServices: () -> Unit,
    onAddApps: () -> Unit,
    onRemovePin: (String) -> Unit,
    onMovePin: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var missing by remember { mutableStateOf<ShortcutState?>(null) }
    var missingEntry by remember { mutableStateOf<HomeEntry?>(null) }
    var menuEntry by remember { mutableStateOf<HomeEntry?>(null) }
    var detail by remember { mutableStateOf<TrendingItem?>(null) }
    val firstTile = remember { FocusRequester() }
    val byId = remember(shortcuts) { shortcuts.associateBy { it.shortcut.id } }
    val hasShortcuts = entries.isNotEmpty()
    LaunchedEffect(hasShortcuts) {
        if (hasShortcuts) { delay(150); runCatching { firstTile.requestFocus() } }
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 26.dp, end = 23.dp, top = 2.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HomeBanner(trending, onStart = onMoreStreaming, onDetails = { detail = it }) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SPText("Aplicativos", RowSectionTitle)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Gap), contentPadding = PaddingValues(vertical = 6.dp)) {
                    itemsIndexed(entries, key = { _, e -> e.id }) { i, e ->
                        SPTile(
                            label = e.label, packageName = e.packageName, installed = e.installed, brand = Color(e.brandColor),
                            onClick = { val pkg = e.packageName; if (e.installed && pkg != null) onOpen(pkg) else missingEntry = e },
                            onLongClick = { menuEntry = e },
                            modifier = Modifier.width(133.dp).height(75.dp).then(if (i == 0) Modifier.focusRequester(firstTile) else Modifier),
                        )
                    }
                    item { SPActionTile("Adicionar", SPIconKind.PLUS, onAddApps, Modifier.width(100.dp).height(75.dp)) }
                    item { SPActionTile("Mais Apps", SPIconKind.DOTS, onMoreApps, Modifier.width(100.dp).height(75.dp)) }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SPText("Streaming", RowSectionTitle)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                    listOf("netflix", "prime", "disney", "globoplay", "hbomax", "youtube").mapNotNull { byId[it] }.forEach { s ->
                        ShortcutTile(s, onOpen, { missing = s }, Modifier.weight(1f).height(70.dp))
                    }
                    SPActionTile("Canais ao Vivo", SPIconKind.TV, onLive, Modifier.weight(1f).height(70.dp))
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                SPActionTile("Filmes e séries IPTV", SPIconKind.STREAMING, onLibrary, Modifier.weight(1f).height(70.dp))
                SPActionTile("Catálogos Netflix e outros", SPIconKind.STREAMING, onServices, Modifier.weight(1f).height(70.dp))
            }
        }
        val movies = trending.filter { it.type == "movie" }
        val series = trending.filter { it.type == "tv" }
        if (movies.isNotEmpty()) item { TrendingRow("Filmes populares", movies) { detail = it } }
        if (series.isNotEmpty()) item { TrendingRow("Séries populares", series) { detail = it } }
        if (favorites.isNotEmpty()) item { AppsRow("Favoritos", favorites, onOpen) }
        if (recents.isNotEmpty()) item { AppsRow("Usados recentemente", recents, onOpen) }

        item {
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.CenterEnd) {
                SPText("STREET PLAY  //  SEMPRE COM VOCÊ", TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LocalAccent.current))
            }
        }
    }

    missing?.let { s ->
        SPDialog("${s.shortcut.name} não está instalado", onDismiss = { missing = null }) {
            SPText("Instale pela loja oficial do seu aparelho. O Street Play não distribui aplicativos.", SPType.Body, color = SPColors.TextSecondary)
            SPButton("Procurar na loja", { onOpenStoreListing(s.shortcut.packages.first()); missing = null })
            SPButton("Fechar", { missing = null })
        }
    }
    missingEntry?.let { e ->
        SPDialog("${e.label} não está instalado", onDismiss = { missingEntry = null }) {
            SPText("Instale pela loja oficial do seu aparelho. O Street Play não distribui aplicativos.", SPType.Body, color = SPColors.TextSecondary)
            e.storePackage?.let { st -> SPButton("Procurar na loja", { onOpenStoreListing(st); missingEntry = null }) }
            SPButton("Remover da tela inicial", { onRemovePin(e.id); missingEntry = null })
            SPButton("Fechar", { missingEntry = null })
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
private fun ShortcutTile(s: ShortcutState, onOpen: (String) -> Unit, onMissing: () -> Unit, modifier: Modifier) {
    SPTile(
        label = s.shortcut.name, packageName = s.resolvedPackage ?: s.shortcut.packages.first(),
        installed = s.installed, brand = Color(s.shortcut.brandColor),
        onClick = { val pkg = s.resolvedPackage; if (pkg != null) onOpen(pkg) else onMissing() },
        modifier = modifier,
    )
}

@Composable
private fun AppsRow(title: String, apps: List<InstalledApp>, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SPText(title, RowSectionTitle)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Gap), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(apps, key = { it.packageName }) { app ->
                SPTile(app.label, app.packageName, true, LocalAccent.current, { onOpen(app.packageName) }, Modifier.width(114.dp).height(75.dp))
            }
        }
    }
}

@Composable
private fun TrendingRow(title: String, items: List<TrendingItem>, onClick: (TrendingItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SPText(title, RowSectionTitle)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Gap), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(items, key = { it.type + it.id }) { t ->
                SPFocusIndicator(
                    onClick = { onClick(t) }, modifier = Modifier.width(92.dp).height(132.dp),
                    container = SPColors.TileBase, focusedContainer = SPColors.TileFocused,
                    focusScale = 1.06f, idleBorder = SPColors.Border,
                ) {
                    SPRemoteImage(t.posterUrl, Modifier.fillMaxSize()) {
                        SPText(t.title, SPType.Caption, Modifier.align(Alignment.Center).padding(6.dp))
                    }
                }
            }
        }
    }
}

/** Carrossel: slide 0 = marca; os demais = destaques do TMDB (com backdrop). Pausa enquanto o botão tem foco. */
@Composable
private fun HomeBanner(trending: List<TrendingItem>, onStart: () -> Unit, onDetails: (TrendingItem) -> Unit) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(16.dp)
    val slides = remember(trending) { trending.filter { it.backdropUrl != null }.take(4) }
    val count = 1 + slides.size
    var index by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    val shown = if (index < count) index else 0
    LaunchedEffect(count, paused) {
        if (count > 1 && !paused) while (true) { delay(9_000); index = (index + 1) % count }
    }
    Box(
        Modifier.fillMaxWidth().height(214.dp).clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF0A0A0A), Color(0xFF120905), Color(0xFF1C0D04))))
            .border(1.5.dp, Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.35f), accent)), shape),
    ) {
        Crossfade(targetState = shown, animationSpec = tween(if (LocalAnimations.current) 500 else 0), label = "banner") { i ->
            if (i == 0) BrandSlide(onStart) { paused = it }
            else MediaSlide(slides[i - 1], onDetails) { paused = it }
        }
        if (count > 1) {
            Row(Modifier.align(Alignment.TopEnd).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(count) { k ->
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (k == shown) accent else Color(0xFF666666)))
                }
            }
        }
    }
}

@Composable
private fun BannerButton(text: String, onClick: () -> Unit, onFocus: (Boolean) -> Unit) {
    val accent = LocalAccent.current
    SPFocusIndicator(
        onClick = onClick, modifier = Modifier.onFocusChanged { onFocus(it.hasFocus) },
        shape = RoundedCornerShape(10.dp), container = Color.Transparent,
        focusedContainer = accent.copy(alpha = 0.28f), focusScale = 1.05f, idleBorder = accent,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(12.dp)) {
                drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, size.height / 2); lineTo(0f, size.height); close() }, accent)
            }
            Spacer(Modifier.width(8.dp))
            SPText(text, TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White))
        }
    }
}

@Composable
private fun BrandSlide(onStart: () -> Unit, onFocus: (Boolean) -> Unit) {
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawPath(Path().apply { moveTo(w * .50f, 0f); lineTo(w * .66f, 0f); lineTo(w * .52f, h); lineTo(w * .36f, h); close() },
                Brush.linearGradient(listOf(accent.copy(alpha = 0f), accent.copy(alpha = 0.16f))))
            drawPath(Path().apply { moveTo(w * .66f, 0f); lineTo(w * .70f, 0f); lineTo(w * .56f, h); lineTo(w * .52f, h); close() },
                accent.copy(alpha = 0.55f))
            drawPath(Path().apply { moveTo(w * .93f, h); lineTo(w, h * .72f); lineTo(w, h); close() }, accent.copy(alpha = 0.85f))
        }
        Column(Modifier.align(Alignment.CenterStart).padding(start = 44.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SPText("STREET PLAY", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, color = accent))
            SPText("SEU ENTRETENIMENTO", TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, color = Color.White))
            SPText("EM UM SÓ LUGAR", TextStyle(fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, color = accent))
            Spacer(Modifier.height(4.dp))
            SPText("Seus apps oficiais, favoritos\ne atalhos em uma tela só.", TextStyle(fontSize = 14.sp, color = Color(0xFFE0E0E0)))
            Spacer(Modifier.height(8.dp))
            BannerButton("COMECE AGORA", onStart, onFocus)
        }
        Image(
            painter = painterResource(R.drawable.logo_banner), contentDescription = null,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 38.dp).height(176.dp),
            contentScale = ContentScale.Fit, colorFilter = rememberLogoTint(),
        )
    }
}

@Composable
private fun MediaSlide(item: TrendingItem, onDetails: (TrendingItem) -> Unit, onFocus: (Boolean) -> Unit) {
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize()) {
        SPRemoteImage(item.backdropUrl, Modifier.fillMaxSize(), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF2090909), Color(0xB3090909), Color.Transparent))))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 44.dp).width(520.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SPText("EM ALTA AGORA  ·  ${item.typeLabel.uppercase()}", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, color = accent))
            SPText(item.title, TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White), maxLines = 2)
            SPText(item.meta(), TextStyle(fontSize = 13.sp, color = Color(0xFFE0E0E0)))
            SPText(item.overview, TextStyle(fontSize = 13.sp, color = Color(0xFFCFCFCF)), maxLines = 3)
            Spacer(Modifier.height(6.dp))
            BannerButton("VER DETALHES", { onDetails(item) }, onFocus)
        }
    }
}
