package br.com.streetplay.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import br.com.streetplay.core.common.LocalIconLoader
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.InstalledApp
import br.com.streetplay.domain.ShortcutState

/** Escolhe o que aparece na fileira "Aplicativos" da Home. Vale para apps oficiais sugeridos e para qualquer app instalado. */
@Composable
fun AddAppsScreen(
    shortcuts: List<ShortcutState>,
    apps: List<InstalledApp>,
    pins: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()
    val suggested = remember(shortcuts, q) { shortcuts.filter { q.isBlank() || it.shortcut.name.contains(q, true) } }
    val suggestedPkgs = remember(shortcuts) { shortcuts.mapNotNull { it.resolvedPackage }.toSet() }
    val installed = remember(apps, q, suggestedPkgs) {
        apps.filter { it.packageName !in suggestedPkgs && (q.isBlank() || it.label.contains(q, true)) }
    }
    val lite = LocalLite.current
    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = if (lite) 56.dp else SPSpacing.xl, vertical = SPSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SPSectionTitle("Adicionar à tela inicial") }
        item { SPSearchField(query, { query = it }, "Pesquisar aplicativos") }
        item { Header("Sugeridos") }
        items(suggested, key = { "sc:" + it.shortcut.id }) { s ->
            val pin = "sc:" + s.shortcut.id
            PickRow(s.shortcut.name, s.resolvedPackage, s.installed, pin in pins) { onToggle(pin) }
        }
        item { Header("Instalados neste aparelho") }
        if (installed.isEmpty()) item { SPText("Nenhum outro aplicativo encontrado.", SPType.Caption) }
        items(installed, key = { "pkg:" + it.packageName }) { a ->
            val pin = "pkg:" + a.packageName
            PickRow(a.label, a.packageName, true, pin in pins) { onToggle(pin) }
        }
    }
}

@Composable
private fun Header(text: String) =
    SPText(text.uppercase(), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SPColors.TextSecondary), Modifier.padding(top = 12.dp, bottom = 2.dp))

@Composable
private fun PickRow(label: String, pkg: String?, installed: Boolean, pinned: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val loader = LocalIconLoader.current
    val icon by produceState<ImageBitmap?>(null, pkg, installed) { value = if (pkg != null && installed) loader(pkg) else null }
    SPFocusIndicator(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(12.dp),
        container = SPColors.TileBase, focusedContainer = SPColors.TileFocused, focusScale = 1.02f,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                val b = icon
                if (b != null) Image(bitmap = b, contentDescription = null, modifier = Modifier.fillMaxSize())
                else SPText(label.take(1).uppercase(), SPType.Title, color = accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                SPText(label, SPType.Body, maxLines = 1)
                SPText(if (installed) "Instalado" else "Não instalado", SPType.Caption, maxLines = 1)
            }
            SPText(
                if (pinned) "Na tela inicial · OK remove" else "OK adiciona",
                TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (pinned) accent else Color(0xFFB0B0B0)),
            )
        }
    }
}
