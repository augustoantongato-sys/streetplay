package br.com.streetplay.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.streetplay.R
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.InstalledApp
import br.com.streetplay.domain.ShortcutState
import kotlinx.coroutines.delay

private val Gap = 11.dp
private val RowSectionTitle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)

@Composable
fun HomeScreen(
    shortcuts: List<ShortcutState>,
    favorites: List<InstalledApp>,
    recents: List<InstalledApp>,
    onOpen: (String) -> Unit,
    onOpenStoreListing: (String) -> Unit,
    onMoreApps: () -> Unit,
    onMoreStreaming: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var missing by remember { mutableStateOf<ShortcutState?>(null) }
    val firstTile = remember { FocusRequester() }
    val byId = remember(shortcuts) { shortcuts.associateBy { it.shortcut.id } }
    val hasShortcuts = shortcuts.isNotEmpty()
    LaunchedEffect(hasShortcuts) {
        if (hasShortcuts) { delay(150); runCatching { firstTile.requestFocus() } }
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 26.dp, end = 23.dp, top = 2.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HomeBanner(onStart = onMoreStreaming) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SPText("Aplicativos", RowSectionTitle)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                    listOf("youtube", "netflix", "prime", "disney", "hbomax", "spotify", "playstore")
                        .mapNotNull { byId[it] }
                        .forEachIndexed { i, s ->
                            ShortcutTile(
                                s, onOpen, { missing = s },
                                Modifier.weight(1f).height(75.dp).then(if (i == 0) Modifier.focusRequester(firstTile) else Modifier),
                            )
                        }
                    SPActionTile("Mais Apps", SPIconKind.DOTS, onMoreApps, Modifier.weight(1f).height(75.dp))
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SPText("Streaming", RowSectionTitle)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                    listOf("netflix", "prime", "disney", "hbomax", "youtube").mapNotNull { byId[it] }.forEach { s ->
                        ShortcutTile(s, onOpen, { missing = s }, Modifier.weight(1f).height(70.dp))
                    }
                    SPActionTile("Mais streaming", SPIconKind.STREAMING, onMoreStreaming, Modifier.weight(1f).height(70.dp))
                }
            }
        }

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
                SPTile(app.label, app.packageName, true, SPColors.Orange, { onOpen(app.packageName) }, Modifier.width(114.dp).height(75.dp))
            }
        }
    }
}

@Composable
private fun HomeBanner(onStart: () -> Unit) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.fillMaxWidth().height(214.dp).clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF0A0A0A), Color(0xFF120905), Color(0xFF1C0D04))))
            .border(1.5.dp, Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.35f), accent)), shape),
    ) {
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
            SPFocusIndicator(
                onClick = onStart, shape = RoundedCornerShape(10.dp), container = Color.Transparent,
                focusedContainer = accent.copy(alpha = 0.28f), focusScale = 1.05f, idleBorder = accent,
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(12.dp)) {
                        drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, size.height / 2); lineTo(0f, size.height); close() }, accent)
                    }
                    Spacer(Modifier.width(8.dp))
                    SPText("COMECE AGORA", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }
            }
        }
        Image(
            painter = painterResource(R.drawable.logo_banner), contentDescription = null,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 38.dp).height(176.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
