package br.com.streetplay.features.live

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.GroupKind
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.LiveGroup

/** Tela de canais no estilo de um app de IPTV: grupos à esquerda, lista numerada à direita. */
@Composable
fun LiveScreen(
    groups: List<LiveGroup>,
    filters: LiveFilters,
    channels: List<LiveChannel>,
    favorites: Set<String>,
    loading: Boolean,
    progress: String,
    stats: LiveStats,
    onSelectGroup: (String) -> Unit,
    onQuery: (String) -> Unit,
    onToggleShowAll: () -> Unit,
    onPlay: (Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lite = LocalLite.current
    if (groups.isEmpty()) {
        SPEmptyState(
            if (loading) "Carregando canais…" else "Nenhum canal disponível",
            if (loading) progress.ifBlank { "Buscando a lista completa" } else "Verifique a internet e abra de novo.",
            modifier,
        )
        return
    }
    val main = groups.filter { it.kind == GroupKind.ALL || it.kind == GroupKind.FAVORITES || it.id == "c:BR" }
    val cats = groups.filter { it.kind == GroupKind.CATEGORY }
    val countries = groups.filter { it.kind == GroupKind.COUNTRY && it.id != "c:BR" }

    Row(
        modifier.fillMaxSize().padding(horizontal = if (lite) 40.dp else 26.dp).padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LazyColumn(Modifier.width(250.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(3.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            item { SPText("Canais ao vivo", SPType.Title, Modifier.padding(bottom = 8.dp)) }
            items(main, key = { it.id }) { GroupRow(it, it.id == filters.group) { onSelectGroup(it.id) } }
            item { GroupHeader("Categorias") }
            items(cats, key = { it.id }) { GroupRow(it, it.id == filters.group) { onSelectGroup(it.id) } }
            item { GroupHeader("Países") }
            items(countries, key = { it.id }) { GroupRow(it, it.id == filters.group) { onSelectGroup(it.id) } }
        }

        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SPSearchField(filters.query, onQuery, "Buscar canal", Modifier.weight(1f))
                if (stats.dead > 0) SPButton(if (filters.showAll) "Ocultar fora do ar" else "Mostrar fora do ar", onToggleShowAll)
            }
            if (!lite) SPText(
                when {
                    stats.checking -> "Verificando canais… ${stats.alive} no ar · ${stats.dead} fora do ar"
                    stats.dead > 0 -> "${channels.size} canais · ${stats.dead} fora do ar ${if (filters.showAll) "(visíveis)" else "(ocultos)"} · Segure OK para favoritar"
                    else -> "${channels.size} canais · Segure OK para favoritar"
                },
                SPType.Caption,
            )
            if (channels.isEmpty()) {
                SPEmptyState(
                    if (filters.group == "fav") "Nenhum favorito ainda" else "Nenhum canal aqui",
                    if (filters.group == "fav") "Segure OK em um canal para favoritar." else "Tente outro grupo ou outra busca.",
                )
            } else {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    itemsIndexed(channels, key = { _, c -> c.id }) { i, c ->
                        ChannelRow(i + 1, c, c.id in favorites, onClick = { onPlay(i) }, onLong = { onToggleFavorite(c.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(text: String) =
    SPText(text.uppercase(), TextStyle(fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = SPColors.TextSecondary), Modifier.padding(top = 14.dp, bottom = 4.dp, start = 6.dp))

@Composable
private fun GroupRow(g: LiveGroup, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    SPFocusIndicator(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        container = if (selected) accent.copy(alpha = 0.18f) else Color.Transparent,
        focusedContainer = SPColors.CardFocused, focusScale = 1.02f,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SPText(g.label, SPType.Label, Modifier.weight(1f), if (selected) accent else SPColors.TextPrimary, 1)
            SPText(g.count.toString(), SPType.Caption)
        }
    }
}

@Composable
private fun ChannelRow(number: Int, ch: LiveChannel, fav: Boolean, onClick: () -> Unit, onLong: () -> Unit) {
    val accent = LocalAccent.current
    val lite = LocalLite.current
    SPFocusIndicator(
        onClick = onClick, onLongClick = onLong, modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(12.dp), container = SPColors.TileBase, focusedContainer = SPColors.TileFocused, focusScale = 1.02f,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            SPText(number.toString().padStart(3, '0'), TextStyle(fontSize = 14.sp, color = SPColors.TextSecondary), Modifier.width(46.dp))
            SPRemoteImage(ch.logo, Modifier.size(38.dp), ContentScale.Fit) {
                SPText(ch.name.take(1).uppercase(), SPType.Label, Modifier.align(Alignment.Center), accent)
            }
            Spacer(Modifier.width(14.dp))
            SPText(ch.name, SPType.Body, Modifier.weight(1f), maxLines = 1)
            if (!lite && ch.quality != null) SPText(ch.quality, SPType.Caption, Modifier.padding(start = 8.dp))
            if (fav) { Spacer(Modifier.width(10.dp)); SPIcon(SPIconKind.FAVORITES, tint = accent, size = 16.dp) }
        }
    }
}
