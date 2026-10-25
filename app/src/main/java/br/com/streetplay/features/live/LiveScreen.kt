package br.com.streetplay.features.live

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.GroupKind
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.LiveGroup
import kotlinx.coroutines.launch
import java.text.Normalizer

/** Tela de canais no estilo de um app de IPTV: grupos à esquerda, lista numerada e índice A–Z à direita. */
@Composable
fun LiveScreen(
    groups: List<LiveGroup>,
    filters: LiveFilters,
    channels: List<LiveChannel>,
    status: Map<String, Boolean>,
    favorites: Set<String>,
    loading: Boolean,
    progress: String,
    onSelectGroup: (String) -> Unit,
    onQuery: (String) -> Unit,
    onVisibleFrom: (Int) -> Unit,
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val letters = remember(channels) { buildLetterIndex(channels) }

    LaunchedEffect(filters.group, filters.query) { listState.scrollToItem(0) }
    LaunchedEffect(listState, channels) { snapshotFlow { listState.firstVisibleItemIndex }.collect { onVisibleFrom(it) } }

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
            SPSearchField(filters.query, onQuery, "Buscar canal")
            if (!lite) SPText("${channels.size} canais · Segure OK para favoritar · Os que estão fora do ar aparecem esmaecidos", SPType.Caption)
            if (channels.isEmpty()) {
                SPEmptyState(
                    if (filters.group == "fav") "Nenhum favorito ainda" else "Nenhum canal aqui",
                    if (filters.group == "fav") "Segure OK em um canal para favoritar." else "Tente outro grupo ou outra busca.",
                )
            } else {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyColumn(Modifier.weight(1f).fillMaxHeight(), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                        itemsIndexed(channels, key = { _, c -> c.id }) { i, c ->
                            ChannelRow(i + 1, c, status[c.url], c.id in favorites, onClick = { onPlay(i) }, onLong = { onToggleFavorite(c.id) })
                        }
                    }
                    if (channels.size > 40 && letters.size > 1) {
                        LazyColumn(Modifier.width(30.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                            items(letters, key = { it.first }) { (letter, index) ->
                                SPFocusIndicator(
                                    onClick = { scope.launch { listState.scrollToItem(index) } }, shape = CircleShape,
                                    container = Color.Transparent, focusedContainer = SPColors.CardFocused, focusScale = 1.15f,
                                ) {
                                    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                        SPText(letter.toString(), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SPColors.TextPrimary))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Primeira posição de cada letra (A–Z; o resto vira "#"). */
internal fun buildLetterIndex(channels: List<LiveChannel>): List<Pair<Char, Int>> {
    val first = LinkedHashMap<Char, Int>()
    channels.forEachIndexed { i, c -> first.putIfAbsent(letterOf(c.name), i) }
    return first.entries.sortedBy { if (it.key == '#') 0.toChar() else it.key }.map { it.key to it.value }
}

internal fun letterOf(name: String): Char {
    val c = Normalizer.normalize(name.trim().take(1), Normalizer.Form.NFD).firstOrNull()?.uppercaseChar()
    return if (c != null && c in 'A'..'Z') c else '#'
}

@Composable
private fun GroupHeader(text: String) =
    SPText(text.uppercase(), TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SPColors.TextSecondary), Modifier.padding(top = 14.dp, bottom = 4.dp, start = 6.dp))

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
private fun ChannelRow(number: Int, ch: LiveChannel, alive: Boolean?, fav: Boolean, onClick: () -> Unit, onLong: () -> Unit) {
    val accent = LocalAccent.current
    val lite = LocalLite.current
    val dead = alive == false
    SPFocusIndicator(
        onClick = onClick, onLongClick = onLong, modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(12.dp), container = SPColors.TileBase, focusedContainer = SPColors.TileFocused, focusScale = 1.02f,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp).alpha(if (dead) 0.45f else 1f), verticalAlignment = Alignment.CenterVertically) {
            SPText(number.toString().padStart(3, '0'), TextStyle(fontSize = 14.sp, color = SPColors.TextSecondary), Modifier.width(46.dp))
            SPRemoteImage(ch.logo, Modifier.size(38.dp), ContentScale.Fit) {
                SPText(ch.name.take(1).uppercase(), SPType.Label, Modifier.align(Alignment.Center), accent)
            }
            Spacer(Modifier.width(14.dp))
            SPText(ch.name, SPType.Body, Modifier.weight(1f), maxLines = 1)
            if (dead) SPText("Fora do ar", SPType.Caption, Modifier.padding(start = 8.dp))
            else if (!lite && ch.quality != null) SPText(ch.quality, SPType.Caption, Modifier.padding(start = 8.dp))
            if (fav) { Spacer(Modifier.width(10.dp)); SPIcon(SPIconKind.FAVORITES, tint = accent, size = 16.dp) }
        }
    }
}
