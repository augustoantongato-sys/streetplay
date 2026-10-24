package br.com.streetplay.features.applications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.AppCategory
import br.com.streetplay.domain.InstalledApp

@Composable
fun AppsScreen(
    apps: List<InstalledApp>,
    favorites: List<String>,
    loading: Boolean,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<AppCategory?>(null) }
    val filtered = remember(apps, query, category) {
        apps.filter { (category == null || it.category == category) && it.label.contains(query.trim(), ignoreCase = true) }
    }
    val present = remember(apps) { AppCategory.entries.filter { c -> apps.any { it.category == c } } }

    Column(modifier.fillMaxSize().padding(horizontal = SPSpacing.xl).padding(top = SPSpacing.lg), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SPSectionTitle("Aplicativos")
        SPSearchField(query, { query = it }, "Pesquisar aplicativos")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(SPSpacing.sm), contentPadding = PaddingValues(vertical = 6.dp)) {
            item { SPButton("Todos", { category = null }, selected = category == null) }
            items(present) { c -> SPButton(c.label, { category = c }, selected = category == c) }
        }
        SPText("Segure OK em um app para favoritar", SPType.Caption)
        when {
            loading && apps.isEmpty() -> SPEmptyState("Carregando…", "Procurando seus aplicativos")
            filtered.isEmpty() -> SPEmptyState("Nenhum aplicativo encontrado", "Tente outro termo ou categoria.")
            else -> AppGrid(filtered, favorites, reorderable = false, onOpen = onOpen, onToggleFavorite = onToggleFavorite, onMove = { _, _ -> })
        }
    }
}
