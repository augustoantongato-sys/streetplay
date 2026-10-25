package br.com.streetplay.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.streetplay.data.CatalogRepository
import br.com.streetplay.domain.TrendingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiscoverViewModel(private val repo: CatalogRepository) : ViewModel() {
    private val _trending = MutableStateFlow<List<TrendingItem>>(emptyList())
    val trending: StateFlow<List<TrendingItem>> = _trending.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { _trending.value = repo.trending() }
    }

    /** Chamado em onResume: tenta de novo só se ainda não carregou (ex.: TV ligou antes do Wi-Fi). */
    fun refreshIfEmpty() { if (_trending.value.isEmpty()) refresh() }
}
