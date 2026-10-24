package br.com.streetplay.features.entertainment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.AppCategory
import br.com.streetplay.domain.InstalledApp
import br.com.streetplay.domain.ShortcutState

/** Área de atalhos e organização. Não lista catálogos de terceiros. */
@Composable
fun EntertainmentScreen(
    shortcuts: List<ShortcutState>,
    apps: List<InstalledApp>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val streaming = remember(shortcuts) { shortcuts.filter { it.installed && it.shortcut.id != "playstore" } }
    val media = remember(apps) { apps.filter { it.category == AppCategory.VIDEO || it.category == AppCategory.AUDIO } }
    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = SPSpacing.xl, vertical = SPSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SPSpacing.lg),
    ) {
        item { SPSectionTitle("Filmes e séries") }
        item {
            SPCard {
                SPText("Seus atalhos de entretenimento", SPType.Body)
                SPText("O Street Play abre os apps oficiais instalados. Ele não hospeda nem lista catálogos de serviços de terceiros.", SPType.Caption)
            }
        }
        if (streaming.isEmpty() && media.isEmpty()) {
            item { SPText("Nenhum app de streaming ou mídia instalado. Veja a seção Loja.", SPType.Body, color = SPColors.TextSecondary) }
        }
        if (streaming.isNotEmpty()) item {
            Column {
                SPSectionTitle("Streaming")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(SPSpacing.md), contentPadding = PaddingValues(vertical = 14.dp)) {
                    items(streaming, key = { it.shortcut.id }) { s ->
                        SPAppCard(s.shortcut.name, s.resolvedPackage, true, large = true, onClick = { s.resolvedPackage?.let(onOpen) })
                    }
                }
            }
        }
        if (media.isNotEmpty()) item {
            Column {
                SPSectionTitle("Vídeo e música instalados")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(SPSpacing.md), contentPadding = PaddingValues(vertical = 14.dp)) {
                    items(media, key = { it.packageName }) { a -> SPAppCard(a.label, a.packageName, true, onClick = { onOpen(a.packageName) }) }
                }
            }
        }
    }
}
