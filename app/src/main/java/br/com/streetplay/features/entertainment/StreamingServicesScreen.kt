package br.com.streetplay.features.entertainment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.streetplay.StreetPlayApp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.TrendingItem
import kotlinx.coroutines.launch

private data class StreamingProvider(val id: Int, val name: String)
private val streamingProviders = listOf(
    StreamingProvider(8, "Netflix"),
    StreamingProvider(119, "Prime Video"),
    StreamingProvider(337, "Disney+"),
    StreamingProvider(1899, "Max"),
    StreamingProvider(350, "Apple TV+"),
    StreamingProvider(307, "Globoplay"),
    StreamingProvider(531, "Paramount+"),
)

@Composable
fun StreamingServicesScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as StreetPlayApp
    val repo = remember { app.container.catalog }
    val provider = remember { mutableStateOf(streamingProviders.first()) }
    var type by remember { mutableStateOf("movie") }
    var titles by remember { mutableStateOf<List<TrendingItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true
            message = "Consultando disponibilidade no Brasil…"
            try {
                titles = repo.providerTitles(provider.value.id, type)
                message = if (titles.isEmpty()) "Nenhum título retornado. Confira se a chave TMDB está configurada e tente novamente." else ""
            } catch (e: Exception) {
                titles = emptyList()
                message = e.message ?: "Não foi possível consultar os serviços."
            } finally { loading = false }
        }
    }
    LaunchedEffect(provider.value.id, type) { load() }

    Column(modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        SPSectionTitle("Serviços de streaming")
        SPText("Explore filmes e séries associados aos catálogos disponíveis no Brasil. O TMDB fornece informações e disponibilidade, não o vídeo para reprodução.", SPType.Caption, color = SPColors.TextSecondary)
        LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SPText("Escolha um serviço", SPType.Label)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    streamingProviders.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { p ->
                                SPButton(if (p.id == provider.value.id) "• ${p.name}" else p.name) { provider.value = p }
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SPButton(if (type == "movie") "Filmes •" else "Filmes") { type = "movie" }
                    SPButton(if (type == "tv") "Séries •" else "Séries") { type = "tv" }
                    SPButton("Atualizar") { load() }
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
    }
}
