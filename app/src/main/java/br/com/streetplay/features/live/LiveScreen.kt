package br.com.streetplay.features.live

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.streetplay.core.design.*
import br.com.streetplay.data.remote.Networks
import br.com.streetplay.domain.GroupKind
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.LiveGroup
import br.com.streetplay.domain.NetworkGroup
import kotlinx.coroutines.launch
import java.text.Normalizer

private sealed interface Row {
    val key: String
    data class Net(val g: NetworkGroup, val open: Boolean) : Row { override val key = "n:${g.key}" }
    data class Chan(val ch: LiveChannel, val nested: Boolean, val number: Int?, val uf: String? = null) : Row { override val key = "c:${ch.id}" }
}

/** Canais no estilo IPTV: grupos à esquerda; à direita, emissoras como pastas (Globo, SBT, Band...) com todas as variações dentro. */
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
    onToggleGrouped: () -> Unit,
    onOpenGloboplay: () -> Unit,
    onCheck: (List<LiveChannel>) -> Unit,
    onPlay: (List<LiveChannel>, LiveChannel) -> Unit,
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
    val brazil = groups.filter { it.kind == GroupKind.BRAZIL }
    val streaming = groups.filter { it.id == "streaming:services" }
    val cats = groups.filter { it.kind == GroupKind.CATEGORY && it.id != "adult:18" && it.id != "streaming:services" }
    val countries = groups.filter { it.kind == GroupKind.COUNTRY && it.id != "c:BR" }
    val adult = groups.filter { it.id == "adult:18" }

    val nets = remember(channels, filters.grouped) { if (filters.grouped) Networks.group(channels) else emptyList() }
    var expanded by remember(filters.group) { mutableStateOf(emptySet<String>()) }
    val searching = filters.query.isNotBlank()
    val rows = remember(nets, channels, expanded, filters.grouped, searching) {
        buildList {
            if (!filters.grouped) channels.forEachIndexed { i, c -> add(Row.Chan(c, false, i + 1)) }
            else nets.forEach { g ->
                if (g.channels.size == 1) add(Row.Chan(g.channels[0], false, null))
                else {
                    val open = searching || g.key in expanded
                    add(Row.Net(g, open))
                    if (open) g.channels.forEach { add(Row.Chan(it, true, null, if (g.key == "globo") Networks.stateOf(it) else null)) }
                }
            }
        }
    }
    val playlist = remember(nets, channels, filters.grouped) { if (filters.grouped) nets.flatMap { it.channels } else channels }
    val letters = remember(rows) { rowLetters(rows) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(filters.group, filters.query, filters.grouped) { listState.scrollToItem(0) }
    LaunchedEffect(listState, rows) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { i ->
            onCheck(rows.drop(i).take(40).mapNotNull { (it as? Row.Chan)?.ch })
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth < 600.dp) {
            Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LiveChannelsBanner(Modifier.fillMaxWidth())
                SPSearchField(filters.query, onQuery, "Buscar canal ou emissora", Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    val allGroups = main + brazil + streaming + cats + countries + adult
                    items(allGroups, key = { it.id }) { group ->
                        SPFocusIndicator(
                            onClick = { onSelectGroup(group.id) },
                            shape = RoundedCornerShape(18.dp),
                            container = if (group.id == filters.group) LocalAccent.current.copy(alpha = .22f) else SPColors.Card,
                            focusedContainer = SPColors.CardFocused,
                        ) {
                            Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                SPText(group.label, SPType.Label, color = if (group.id == filters.group) LocalAccent.current else SPColors.TextPrimary, maxLines = 1)
                                Spacer(Modifier.width(6.dp))
                                SPText(group.count.toString(), SPType.Caption)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SPText(if (filters.grouped) "Emissoras e canais" else "Todos os canais", SPType.Title, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    SPButton(if (filters.grouped) "Lista" else "Emissoras", onToggleGrouped)
                }
                if (channels.isEmpty()) {
                    SPEmptyState(if (filters.group == "fav") "Nenhum favorito ainda" else "Nenhum canal aqui",
                        if (loading) progress else "Tente outro grupo ou confira sua conexão.")
                } else {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                        items(rows, key = { it.key }) { row ->
                            when (row) {
                                is Row.Net -> NetworkRow(row) {
                                    expanded = if (row.g.key in expanded) expanded - row.g.key else expanded + row.g.key
                                }
                                is Row.Chan -> ChannelRow(null, row.ch, status[row.ch.url], row.ch.id in favorites, row.uf,
                                    onClick = { onPlay(playlist, row.ch) }, onLong = { onToggleFavorite(row.ch.id) })
                            }
                        }
                    }
                }
            }
        } else {
        Row(
            modifier.fillMaxSize().padding(horizontal = if (lite) 40.dp else 26.dp).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LazyColumn(Modifier.width(250.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(3.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                item { SPText("Canais ao vivo", SPType.Title, Modifier.padding(bottom = 8.dp)) }
                items(main, key = { it.id }) { GroupRow(it, it.id == filters.group) { onSelectGroup(it.id) } }
                if (brazil.isNotEmpty()) item { GroupHeader("Brasil") }
                items(brazil, key = { it.id }) { GroupRow(it, it.id == filters.group) { onSelectGroup(it.id) } }
                if (streaming.isNotEmpty()) {
                    item { GroupHeader("Streaming") }
                    items(streaming, key = { it.id }) { GroupRow(it, it.id == filters.group) { onSelectGroup(it.id) } }
                }
                item { GroupHeader("Categorias") }
                items(cats, key = { it.id }) { GroupRow(it, it.id == filters.group) { onSelectGroup(it.id) } }
                item { GroupHeader("Países") }
                items(countries, key = { it.id }) { GroupRow(it, it.id == filters.group) { onSelectGroup(it.id) } }
                if (adult.isNotEmpty()) {
                    item { GroupHeader("Conteúdo adulto") }
                    items(adult, key = { it.id }) { GroupRow(it, it.id == filters.group) { onSelectGroup(it.id) } }
                }
            }
    
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LiveChannelsBanner(Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SPSearchField(filters.query, onQuery, "Buscar canal ou emissora", Modifier.weight(1f))
                    SPButton(if (filters.grouped) "Ver em lista" else "Ver por emissora", onToggleGrouped)
                }
                if (!lite) SPText(
                    if (filters.grouped) "${nets.size} emissoras · ${channels.size} canais · OK abre a emissora · Segure OK para favoritar"
                    else "${channels.size} canais · Segure OK para favoritar · Fora do ar aparece esmaecido",
                    SPType.Caption,
                )
                if (channels.isEmpty()) {
                    SPEmptyState(
                        if (filters.group == "fav") "Nenhum favorito ainda" else "Nenhum canal aqui",
                        if (filters.group == "fav") "Segure OK em um canal para favoritar." else "Tente outro grupo ou outra busca.",
                    )
                } else {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LazyColumn(Modifier.weight(1f).fillMaxHeight(), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                            items(rows, key = { it.key }) { row ->
                                when (row) {
                                    is Row.Net -> NetworkRow(row) {
                                        expanded = if (row.g.key in expanded) expanded - row.g.key else expanded + row.g.key
                                    }
                                    is Row.Chan -> Box(Modifier.padding(start = if (row.nested) 40.dp else 0.dp)) {
                                        ChannelRow(
                                            row.number, row.ch, status[row.ch.url], row.ch.id in favorites, row.uf,
                                            onClick = { onPlay(playlist, row.ch) }, onLong = { onToggleFavorite(row.ch.id) },
                                        )
                                    }
                                }
                            }
                        }
                        if (rows.size > 30 && letters.size > 1) {
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
    }
}

/** Primeira linha de cada letra, considerando só as linhas de nível principal (emissoras e canais soltos). */
private fun rowLetters(rows: List<Row>): List<Pair<Char, Int>> {
    val first = LinkedHashMap<Char, Int>()
    rows.forEachIndexed { i, r ->
        val name = when (r) { is Row.Net -> r.g.name; is Row.Chan -> if (r.nested) null else r.ch.name }
        if (name != null) first.putIfAbsent(letterOf(name), i)
    }
    return first.entries.sortedBy { if (it.key == '#') 0.toChar() else it.key }.map { it.key to it.value }
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

/** A "pasta" de uma emissora: logo, nome, quantidade de canais e seta. */
@Composable
private fun NetworkRow(row: Row.Net, onClick: () -> Unit) {
    val accent = LocalAccent.current
    SPFocusIndicator(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(14.dp), container = SPColors.Card, focusedContainer = SPColors.CardFocused, focusScale = 1.02f,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            SPRemoteImage(row.g.logo, Modifier.size(42.dp), ContentScale.Fit) {
                SPText(row.g.name.take(1).uppercase(), SPType.Title, Modifier.align(Alignment.Center), accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                SPText(row.g.name, TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SPColors.TextPrimary), maxLines = 1)
                SPText("${row.g.channels.size} canais" + if (row.g.states.isNotEmpty()) " · " + row.g.states.joinToString(" ") else "", SPType.Caption, maxLines = 1)
            }
            Canvas(Modifier.size(14.dp)) {
                val w = size.width; val h = size.height
                drawPath(
                    Path().apply {
                        if (row.open) { moveTo(0f, h * .25f); lineTo(w, h * .25f); lineTo(w / 2, h * .85f) }
                        else { moveTo(w * .2f, 0f); lineTo(w * .85f, h / 2); lineTo(w * .2f, h) }
                        close()
                    },
                    accent,
                )
            }
        }
    }
}

@Composable
private fun ChannelRow(number: Int?, ch: LiveChannel, alive: Boolean?, fav: Boolean, uf: String?, onClick: () -> Unit, onLong: () -> Unit) {
    val accent = LocalAccent.current
    val lite = LocalLite.current
    val dead = alive == false
    SPFocusIndicator(
        onClick = onClick, onLongClick = onLong, modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(12.dp), container = SPColors.TileBase, focusedContainer = SPColors.TileFocused, focusScale = 1.02f,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp).alpha(if (dead) 0.45f else 1f), verticalAlignment = Alignment.CenterVertically) {
            if (number != null) SPText(number.toString().padStart(3, '0'), TextStyle(fontSize = 14.sp, color = SPColors.TextSecondary), Modifier.width(46.dp))
            SPRemoteImage(ch.logo, Modifier.size(38.dp), ContentScale.Fit) {
                SPText(ch.name.take(1).uppercase(), SPType.Label, Modifier.align(Alignment.Center), accent)
            }
            Spacer(Modifier.width(14.dp))
            SPText(ch.name, SPType.Body, Modifier.weight(1f), maxLines = 1)
            if (uf != null) SPText(uf, TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent), Modifier.padding(start = 8.dp))
            if (dead) SPText("Fora do ar", SPType.Caption, Modifier.padding(start = 8.dp))
            else if (ch.external) SPText(if (ch.url.contains("twitch", true)) "Twitch" else "YouTube", SPType.Caption, Modifier.padding(start = 8.dp))
            else if (!lite && ch.quality != null) SPText(ch.quality, SPType.Caption, Modifier.padding(start = 8.dp))
            if (fav) { Spacer(Modifier.width(10.dp)); SPIcon(SPIconKind.FAVORITES, tint = accent, size = 16.dp) }
        }
    }
}


@Composable
private fun LiveChannelsBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(88.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(Color(0xFFFF8A00), Color(0xFFFF5A00), Color(0xFFD83B00))
                ),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(52.dp).background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            SPText("▶", TextStyle(fontSize = 23.sp, fontWeight = FontWeight.Black, color = Color.White))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SPText("STREET PLAY", SPType.Label, color = Color.White, maxLines = 1)
            SPText("CANAIS AO VIVO", SPType.Title, color = Color.White, maxLines = 1)
            SPText("Sua programação em tempo real", SPType.Caption, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
        }
    }
}
