package br.com.streetplay.features.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.streetplay.data.CatalogRepository
import br.com.streetplay.data.remote.StreamChecker
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.LiveSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class LiveStats(val total: Int = 0, val alive: Int = 0, val dead: Int = 0, val checking: Boolean = false)

class LiveViewModel(private val repo: CatalogRepository) : ViewModel() {
    private val _source = MutableStateFlow(LiveSource.BRAZIL)
    val source: StateFlow<LiveSource> = _source.asStateFlow()
    private val _channels = MutableStateFlow<List<LiveChannel>>(emptyList())
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    private val _status = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _checking = MutableStateFlow(false)
    private val _showAll = MutableStateFlow(false)
    val showAll: StateFlow<Boolean> = _showAll.asStateFlow()
    private val _counts = MutableStateFlow<Map<LiveSource, Int>>(emptyMap())
    val counts: StateFlow<Map<LiveSource, Int>> = _counts.asStateFlow()
    private var job: Job? = null
    private var checkJob: Job? = null

    /** Canais exibidos: esconde os confirmados fora do ar (a menos que "mostrar todos"). */
    val visible: StateFlow<List<LiveChannel>> = combine(_channels, _status, _showAll) { ch, st, all ->
        if (all) ch else ch.filter { st[it.url] != false }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<LiveStats> = combine(_channels, _status, _checking) { ch, st, chk ->
        LiveStats(ch.size, ch.count { st[it.url] == true }, ch.count { st[it.url] == false }, chk)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LiveStats())

    fun load(source: LiveSource = _source.value) {
        _source.value = source
        job?.cancel(); checkJob?.cancel()
        job = viewModelScope.launch {
            _loading.value = true
            _channels.value = emptyList()
            val list = repo.channels(source)
            _channels.value = list
            _counts.update { it + (source to list.size) }
            _loading.value = false
            _loaded.value = true
            check(source, list)
        }
    }

    fun loadIfNeeded() {
        if (!_loaded.value && !_loading.value) {
            load()
            viewModelScope.launch { preloadCounts() }
        }
    }

    fun toggleShowAll() { _showAll.update { !it } }

    private suspend fun preloadCounts() {
        for (s in LiveSource.entries) {
            if (_counts.value[s] == null) _counts.update { it + (s to repo.channels(s).size) }
        }
    }

    private fun check(source: LiveSource, list: List<LiveChannel>) {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            val known = repo.loadStatus(source).filterKeys { url -> list.any { it.url == url } }
            _status.value = known
            val pending = list.filter { it.url !in known }
            if (pending.isEmpty()) return@launch
            _checking.value = true
            try {
                val gate = Semaphore(6)
                coroutineScope {
                    pending.map { ch ->
                        async { gate.withPermit { val ok = StreamChecker.isAlive(ch); _status.update { it + (ch.url to ok) } } }
                    }.awaitAll()
                }
                repo.saveStatus(source, _status.value)
            } finally {
                _checking.value = false
            }
        }
    }
}
