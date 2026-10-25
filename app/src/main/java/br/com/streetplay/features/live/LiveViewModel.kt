package br.com.streetplay.features.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.streetplay.data.CatalogRepository
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.LiveSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveViewModel(private val repo: CatalogRepository) : ViewModel() {
    private val _source = MutableStateFlow(LiveSource.BRAZIL)
    val source: StateFlow<LiveSource> = _source.asStateFlow()
    private val _channels = MutableStateFlow<List<LiveChannel>>(emptyList())
    val channels: StateFlow<List<LiveChannel>> = _channels.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()
    private var job: Job? = null

    fun load(source: LiveSource = _source.value) {
        _source.value = source
        job?.cancel()
        job = viewModelScope.launch {
            _loading.value = true
            _channels.value = emptyList()
            _channels.value = repo.channels(source)
            _loading.value = false
            _loaded.value = true
        }
    }

    fun loadIfNeeded() { if (!_loaded.value && !_loading.value) load() }
}
