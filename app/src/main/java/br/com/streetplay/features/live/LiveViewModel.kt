package br.com.streetplay.features.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.streetplay.core.storage.PrefsStore
import br.com.streetplay.data.CatalogRepository
import br.com.streetplay.data.remote.LiveLabels
import br.com.streetplay.data.remote.Networks
import br.com.streetplay.data.remote.StreamChecker
import br.com.streetplay.domain.GroupKind
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.LiveGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class LiveFilters(val group: String = "c:BR", val query: String = "", val grouped: Boolean = true)

/** Lista de canais no estilo IPTV: todos, favoritos, categorias e países, com busca. Nenhum canal é escondido. */
class LiveViewModel(private val repo: CatalogRepository, private val prefs: PrefsStore) : ViewModel() {
    private val _all = MutableStateFlow<List<LiveChannel>>(emptyList())
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress.asStateFlow()
    private val _filters = MutableStateFlow(LiveFilters())
    val filters: StateFlow<LiveFilters> = _filters.asStateFlow()
    private val _status = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    /** url do canal -> responde? (só existe para os canais já verificados) */
    val status: StateFlow<Map<String, Boolean>> = _status.asStateFlow()
    private val gate = Semaphore(6)
    private var windowJob: Job? = null

    val favorites: StateFlow<Set<String>> = prefs.liveFavorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val groups: StateFlow<List<LiveGroup>> = combine(_all, favorites) { all, favs -> buildGroups(all, favs) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val baseList: StateFlow<List<LiveChannel>> =
        combine(_all, _filters.map { it.group }.distinctUntilChanged(), favorites) { all, g, favs -> filterGroup(all, g, favs) }
            .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val visible: StateFlow<List<LiveChannel>> = combine(baseList, _filters) { base, f ->
        if (f.query.isBlank()) base else base.filter { it.name.contains(f.query.trim(), ignoreCase = true) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun loadIfNeeded() { if (_all.value.isEmpty() && !_loading.value) reload() }

    fun reload() {
        viewModelScope.launch {
            _loading.value = true
            _status.value = repo.loadStatus()
            _all.value = repo.liveChannels { _progress.value = it }
            _loading.value = false
            val g = _filters.value.group
            if (g != "fav" && filterGroup(_all.value, g, favorites.value).isEmpty()) _filters.update { it.copy(group = "all") }
        }
    }

    /** Países abrem agrupados por emissora (Globo, SBT, Band...); as demais listas, em ordem alfabética. */
    fun selectGroup(id: String) { _filters.update { it.copy(group = id, query = "", grouped = id.startsWith("c:") || id.startsWith("bk:") || id == "br:regional") } }
    fun toggleGrouped() { _filters.update { it.copy(grouped = !it.grouped) } }
    fun setQuery(q: String) { _filters.update { it.copy(query = q) } }
    fun toggleFavorite(id: String) { viewModelScope.launch { prefs.toggleLiveFavorite(id) } }

    /** Verifica só os canais que estão na tela, conforme o usuário rola a lista. */
    fun checkChannels(list: List<LiveChannel>) {
        windowJob?.cancel()
        windowJob = viewModelScope.launch {
            delay(500)
            val slice = list.filter { !it.external && it.url !in _status.value }.take(40)
            if (slice.isEmpty()) return@launch
            coroutineScope {
                slice.map { ch ->
                    async { gate.withPermit { val ok = StreamChecker.isAlive(ch); _status.update { it + (ch.url to ok) } } }
                }.awaitAll()
            }
            repo.saveStatus(_status.value)
        }
    }
}

internal fun filterGroup(all: List<LiveChannel>, group: String, favs: Set<String>): List<LiveChannel> = when {
    group == "br:regional" -> all.filter { it.country == "BR" && !Networks.isMajorBrazilian(it) }
    group.startsWith("bk:") -> all.filter { it.country == "BR" && group.substring(3) in it.categories }
    group == "all" -> all
    group == "fav" -> all.filter { it.id in favs }
    group.startsWith("c:") -> all.filter { it.country == group.substring(2) }
    group.startsWith("k:") -> all.filter { group.substring(2) in it.categories }
    else -> all
}

internal fun buildGroups(all: List<LiveChannel>, favs: Set<String>): List<LiveGroup> {
    if (all.isEmpty()) return emptyList()
    val main = listOf(
        LiveGroup("all", "Todos os canais", all.size, GroupKind.ALL),
        LiveGroup("fav", "Favoritos", all.count { it.id in favs }, GroupKind.FAVORITES),
        LiveGroup("c:BR", "Brasil", all.count { it.country == "BR" }, GroupKind.COUNTRY),
    )
    val cats = all.flatMap { it.categories }.groupingBy { it }.eachCount().entries
        .sortedByDescending { it.value }
        .map { LiveGroup("k:${it.key}", LiveLabels.category(it.key), it.value, GroupKind.CATEGORY) }
    val countries = all.groupingBy { it.country }.eachCount().filterKeys { it.isNotBlank() && it != "BR" }.entries
        .map { LiveGroup("c:${it.key}", LiveLabels.country(it.key), it.value, GroupKind.COUNTRY) }
        .sortedBy { it.label }
    val br = all.filter { it.country == "BR" }
    val brKinds = listOf("news", "sports", "kids", "entertainment", "religious").mapNotNull { k ->
        val n = br.count { k in it.categories }
        if (n > 0) LiveGroup("bk:$k", LiveLabels.category(k), n, GroupKind.BRAZIL) else null
    }
    val regional = br.count { !Networks.isMajorBrazilian(it) }
    val brazil = brKinds + if (regional > 0) listOf(LiveGroup("br:regional", "Regionais e outros", regional, GroupKind.BRAZIL)) else emptyList()
    return main + brazil + cats + countries
}
