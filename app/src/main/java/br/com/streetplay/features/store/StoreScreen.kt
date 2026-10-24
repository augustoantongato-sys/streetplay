package br.com.streetplay.features.store

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.streetplay.core.design.*
import br.com.streetplay.data.StreamingCatalog
import br.com.streetplay.domain.ShortcutState

/** Direciona para a loja oficial do aparelho. Nunca distribui APKs. */
@Composable
fun StoreScreen(
    shortcuts: List<ShortcutState>,
    onOpen: (String) -> Unit,
    onOpenStoreListing: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playStore = shortcuts.firstOrNull { it.shortcut.id == "playstore" }
    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = SPSpacing.xl, vertical = SPSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SPSpacing.md),
    ) {
        item { SPSectionTitle("Loja") }
        item {
            SPCard {
                if (playStore?.resolvedPackage != null) {
                    SPText("Google Play Store disponível neste aparelho.", SPType.Body)
                    SPButton("Abrir Google Play Store", { onOpen(StreamingCatalog.PLAY_STORE_PACKAGE) })
                } else {
                    SPText("Este aparelho não tem a Google Play Store.", SPType.Body)
                    SPText("Use a loja de aplicativos do fabricante. O Street Play não distribui APKs.", SPType.Caption)
                }
            }
        }
        item { SPText("Apps que ainda não estão instalados", SPType.Label, color = SPColors.TextSecondary) }
        val missing = shortcuts.filter { !it.installed && it.shortcut.id != "playstore" }
        if (missing.isEmpty()) item { SPText("Todos os atalhos principais já estão instalados.", SPType.Body) }
        items(missing, key = { it.shortcut.id }) { s ->
            SPSettingsItem(s.shortcut.name, { onOpenStoreListing(s.shortcut.packages.first()) }, subtitle = "Abrir na loja oficial", value = "Procurar")
        }
    }
}
