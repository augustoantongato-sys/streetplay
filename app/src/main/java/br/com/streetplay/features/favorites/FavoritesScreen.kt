package br.com.streetplay.features.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.InstalledApp
import br.com.streetplay.features.applications.AppGrid

@Composable
fun FavoritesScreen(
    apps: List<InstalledApp>,
    favorites: List<String>,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byPkg = remember(apps) { apps.associateBy { it.packageName } }
    val list = remember(apps, favorites) { favorites.mapNotNull { byPkg[it] } }
    Column(modifier.fillMaxSize().padding(horizontal = SPSpacing.xl).padding(top = SPSpacing.lg), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SPSectionTitle("Favoritos")
        if (!LocalLite.current) SPText("Segure OK em um app para reordenar ou remover", SPType.Caption)
        if (list.isEmpty()) SPEmptyState("Nenhum favorito ainda", "Em Aplicativos, segure OK em um app para favoritar.")
        else AppGrid(list, favorites, reorderable = true, onOpen = onOpen, onToggleFavorite = onToggleFavorite, onMove = onMove)
    }
}
