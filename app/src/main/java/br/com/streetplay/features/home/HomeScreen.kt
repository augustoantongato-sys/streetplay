package br.com.streetplay.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.InstalledApp
import br.com.streetplay.domain.ShortcutState
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun HomeScreen(
    shortcuts: List<ShortcutState>,
    favorites: List<InstalledApp>,
    recents: List<InstalledApp>,
    totalApps: Int,
    onOpen: (String) -> Unit,
    onOpenStoreListing: (String) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var missing by remember { mutableStateOf<ShortcutState?>(null) }
    val firstCard = remember { FocusRequester() }
    val hasShortcuts = shortcuts.isNotEmpty()
    LaunchedEffect(hasShortcuts) {
        if (hasShortcuts) { delay(150); runCatching { firstCard.requestFocus() } }
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = SPSpacing.xl, vertical = SPSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SPSpacing.lg),
    ) {
        item { SPHeader(onSettings) }
        item { HomeHero(totalApps, favorites.size) }
        item {
            Column {
                SPSectionTitle("Seus apps")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(SPSpacing.md), contentPadding = PaddingValues(vertical = 14.dp)) {
                    itemsIndexed(shortcuts, key = { _, s -> s.shortcut.id }) { i, s ->
                        SPAppCard(
                            label = s.shortcut.name, packageName = s.resolvedPackage, installed = s.installed, large = true,
                            subtitle = if (s.installed) null else "Não instalado",
                            onClick = { val pkg = s.resolvedPackage; if (pkg != null) onOpen(pkg) else missing = s },
                            modifier = if (i == 0) Modifier.focusRequester(firstCard) else Modifier,
                        )
                    }
                }
            }
        }
        if (favorites.isNotEmpty()) item { AppRow("Favoritos", favorites, onOpen) }
        if (recents.isNotEmpty()) item { AppRow("Usados recentemente", recents, onOpen) }
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
private fun AppRow(title: String, apps: List<InstalledApp>, onOpen: (String) -> Unit) {
    Column {
        SPSectionTitle(title)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(SPSpacing.md), contentPadding = PaddingValues(vertical = 14.dp)) {
            items(apps, key = { it.packageName }) { app ->
                SPAppCard(app.label, app.packageName, installed = true, onClick = { onOpen(app.packageName) })
            }
        }
    }
}

@Composable
private fun HomeHero(total: Int, favorites: Int) {
    val accent = LocalAccent.current
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) { in 5..11 -> "Bom dia"; in 12..17 -> "Boa tarde"; else -> "Boa noite" }
    }
    Box(
        Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(SPRadius.lg))
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.30f), SPColors.Card)))
            .padding(SPSpacing.lg),
    ) {
        Column(Modifier.align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SPText(greeting, SPType.Display)
            SPText("Seu entretenimento. Do seu jeito.", SPType.Title, color = SPColors.TextSecondary)
            SPText("$total aplicativos instalados · $favorites favoritos", SPType.Body, color = accent)
        }
    }
}
