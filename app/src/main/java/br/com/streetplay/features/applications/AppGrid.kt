package br.com.streetplay.features.applications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.InstalledApp

/** Grade de apps. OK abre; segurar OK abre o menu de opções (favoritar / reordenar). */
@Composable
fun AppGrid(
    apps: List<InstalledApp>,
    favorites: List<String>,
    reorderable: Boolean,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPkg by remember { mutableStateOf<String?>(null) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(176.dp), modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 18.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(SPSpacing.md),
        verticalArrangement = Arrangement.spacedBy(SPSpacing.md),
    ) {
        items(apps, key = { it.packageName }) { app ->
            SPAppCard(
                label = app.label, packageName = app.packageName, installed = true,
                subtitle = if (app.packageName in favorites) "★ Favorito" else null,
                onClick = { onOpen(app.packageName) }, onLongClick = { selectedPkg = app.packageName },
            )
        }
    }
    val app = apps.firstOrNull { it.packageName == selectedPkg }
    if (app != null) {
        val isFav = app.packageName in favorites
        val index = apps.indexOf(app)
        SPDialog(app.label, onDismiss = { selectedPkg = null }) {
            SPButton("Abrir", { onOpen(app.packageName); selectedPkg = null })
            SPButton(if (isFav) "Remover dos favoritos" else "Adicionar aos favoritos", {
                onToggleFavorite(app.packageName)
                if (isFav && reorderable) selectedPkg = null
            })
            if (reorderable && isFav) {
                if (index > 0) SPButton("Mover para antes", { onMove(app.packageName, -1) })
                if (index < apps.lastIndex) SPButton("Mover para depois", { onMove(app.packageName, 1) })
            }
            SPButton("Fechar", { selectedPkg = null })
        }
    }
}
