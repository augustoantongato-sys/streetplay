package br.com.streetplay.features.entertainment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.streetplay.StreetPlayApp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.StreamingProvider
import br.com.streetplay.domain.TrendingItem
import kotlinx.coroutines.launch

private data class ServiceLink(val label: String, val aliases: List<String>, val url: String)
private val preferredServices = listOf(
    ServiceLink("Netflix", listOf("netflix"), "https://www.netflix.com/br/"),
    ServiceLink("Prime Video", listOf("prime video", "amazon prime"), "https://www.primevideo.com/"),
    ServiceLink("Disney+", listOf("disney"), "https://www.disneyplus.com/pt-br"),
    ServiceLink("Max", listOf("hbo max", "max"), "https://www.max.com/br/pt"),
    ServiceLink("Apple TV+", listOf("apple tv"), "https://tv.apple.com/br"),
    ServiceLink("Globoplay", listOf("globoplay"), "https://globoplay.globo.com/"),
    ServiceLink("Paramount+", listOf("paramount+", "paramount plus"), "https://www.paramountplus.com/br/"),
)

private fun curatedProviders(all: List<StreamingProvider>): List<StreamingProvider> =
    preferredServices.mapNotNull { service ->
        all.firstOrNull { p ->
            if (service.label == "Max") p.name.equals("Max", true) || p.name.contains("HBO Max", true)
            else service.aliases.any { alias -> p.name.contains(alias, ignoreCase = true) }
        }
    }.distinctBy { it.id }

private fun officialServiceUrl(provider: StreamingProvider): String =
    preferredServices.firstOrNull { service ->
        service.aliases.any { provider.name.contains(it, ignoreCase = true) } ||
            (service.label == "Max" && provider.name.equals("Max", true))
    }?.url ?: "https://www.google.com/search?q=" + Uri.encode(provider.name + " oficial Brasil")

@Composable
fun StreamingServicesScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as StreetPlayApp
    val repo = remember { app.container.catalog }
    var providers by remember { mutableStateOf<List<StreamingProvider>>(emptyList()) }
    var selectedProvider by remember { mutableStateOf<StreamingProvider?>(null) }
    var type by remember { mutableStateOf("movie") }
    var titles by remember { mutableStateOf<List<TrendingItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun loadProviders(forceRefresh: Boolean = false) {
        scope.launch {
            loading = true
            message = "Buscando os serviços disponíveis no Brasil…"
            try {
                providers = curatedProviders(repo.watchProviders(type, forceRefresh))
                if (selectedProvider?.id !in providers.map { it.id }) selectedProvider = providers.firstOrNull()
                message = if (providers.isEmpty()) "Nenhum dos serviços selecionados foi retornado pelo TMDB nesta região." else ""
            } catch (e: Exception) {
                message = e.message ?: "Não foi possível consultar os serviços."
            } finally { loading = false }
        }
    }

    fun loadTitles(forceRefresh: Boolean = false) {
        val selected = selectedProvider ?: return
        scope.launch {
            loading = true
            message = "Consultando títulos de ${selected.name} no Brasil…"
            try {
                titles = repo.providerTitles(selected.id, type, forceRefresh)
                message = if (titles.isEmpty()) "Nenhum título encontrado para este serviço e tipo de conteúdo." else ""
            } catch (e: Exception) {
                titles = emptyList()
                message = e.message ?: "Não foi possível consultar o catálogo."
            } finally { loading = false }
        }
    }

    LaunchedEffect(type) {
        titles = emptyList()
        selectedProvider = null
        loadProviders()
    }
    LaunchedEffect(selectedProvider?.id, type) {
        if (selectedProvider != null) loadTitles()
    }

    Column(modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        SPSectionTitle("Plataformas")
        SPText("Toque em uma logo para abrir a plataforma oficial. Os títulos abaixo são os que o catálogo indica como disponíveis por assinatura no Brasil.", SPType.Caption, color = SPColors.TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            SPButton(if (type == "movie") "Filmes •" else "Filmes", onClick = { type = "movie" })
            SPButton(if (type == "tv") "Séries •" else "Séries", onClick = { type = "tv" })
            SPButton("Atualizar", onClick = { loadProviders(forceRefresh = true); loadTitles(forceRefresh = true) })
        }
        if (providers.isNotEmpty()) {
            LazyColumn(contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { SPText("Escolha um serviço", SPType.Label) }
                items(providers, key = { it.id }) { provider ->
                    SPFocusIndicator(
                        onClick = {
                            selectedProvider = provider
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(officialServiceUrl(provider)))) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    ) {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SPRemoteImage(provider.logoUrl, Modifier.size(52.dp).padding(4.dp))
                            SPText(provider.name, SPType.Label, Modifier.weight(1f))
                            SPText("Abrir", SPType.Caption, color = LocalAccent.current)
                            if (provider.id == selectedProvider?.id) SPText("Selecionado", SPType.Caption, color = LocalAccent.current)
                        }
                    }
                }
                if (loading) item { SPText("Carregando títulos…", SPType.Body) }
                if (message.isNotBlank()) item { SPText(message, SPType.Body, color = SPColors.TextSecondary) }
                items(titles, key = { "${it.type}:${it.id}" }) { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        SPRemoteImage(item.posterUrl, Modifier.width(76.dp).height(110.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            SPText(item.title, SPType.Label, maxLines = 2)
                            SPText((if (item.type == "movie") "Filme" else "Série") + if (item.year.isNotBlank()) " • ${item.year}" else "", SPType.Caption, color = SPColors.TextSecondary)
                            if (item.rating > 0) SPText("Nota TMDB: ${"%.1f".format(item.rating)}", SPType.Caption)
                            SPText(item.overview.ifBlank { "Sinopse não disponível." }, SPType.Caption, color = SPColors.TextSecondary, maxLines = 4)
                        }
                    }
                }
            }
        } else if (message.isNotBlank()) SPText(message, SPType.Body, color = SPColors.TextSecondary)
    }
}
