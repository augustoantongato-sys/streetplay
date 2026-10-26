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
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.data.remote.XtreamEpisode
import br.com.streetplay.data.remote.XtreamSeriesItem
import br.com.streetplay.data.remote.XtreamVodItem
import kotlinx.coroutines.launch

private enum class LibraryTab { MOVIES, SERIES }

/** Biblioteca pessoal: só exibe o catálogo autorizado pelo servidor Xtream configurado pelo usuário. */
@Composable
fun IptvLibraryScreen(onPlay: (LiveChannel) -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as StreetPlayApp
    val account = remember { app.container.xtreamAccountStore.load() }
    val api = remember { app.container.xtreamApi }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(LibraryTab.MOVIES) }
    var movies by remember { mutableStateOf<List<XtreamVodItem>>(emptyList()) }
    var series by remember { mutableStateOf<List<XtreamSeriesItem>>(emptyList()) }
    var episodes by remember { mutableStateOf<List<XtreamEpisode>>(emptyList()) }
    var selectedSeries by remember { mutableStateOf<XtreamSeriesItem?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun refresh() {
        if (account == null) { message = "Conecte sua conta em Configurações > Conta IPTV."; return }
        scope.launch {
            loading = true
            message = "Carregando catálogo do seu provedor…"
            try {
                if (tab == LibraryTab.MOVIES) movies = api.movies(account) else series = api.series(account)
                message = if ((if (tab == LibraryTab.MOVIES) movies.size else series.size) == 0)
                    "Seu provedor não retornou itens nesta seção." else ""
            } catch (e: Exception) {
                message = e.message ?: "Não foi possível carregar o catálogo."
            } finally { loading = false }
        }
    }
    LaunchedEffect(tab, account) { selectedSeries = null; episodes = emptyList(); refresh() }

    Column(modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        SPSectionTitle("Filmes e séries do seu IPTV")
        SPText("Conteúdo fornecido pela sua própria assinatura. A disponibilidade depende do seu provedor.", SPType.Caption, color = SPColors.TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            SPButton(if (tab == LibraryTab.MOVIES) "Filmes • selecionado" else "Filmes", onClick = { tab = LibraryTab.MOVIES })
            SPButton(if (tab == LibraryTab.SERIES) "Séries • selecionado" else "Séries", onClick = { tab = LibraryTab.SERIES })
            SPButton("Atualizar", onClick = { refresh() })
        }
        if (message.isNotBlank()) SPText(message, SPType.Body, color = SPColors.TextSecondary)
        if (loading) SPText("Carregando…", SPType.Body)
        if (selectedSeries != null) {
            SPButton("← Voltar para séries", onClick = { selectedSeries = null; episodes = emptyList() })
            SPSectionTitle(selectedSeries!!.name)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(episodes, key = { it.id }) { ep ->
                    SPFocusIndicator(onClick = {
                        val s = selectedSeries!!
                        val url = api.playbackUrl(account!!, "series", ep.id, ep.extension ?: ep.container)
                        onPlay(LiveChannel("xtream-episode:${ep.id}", "${s.name} • T${ep.season} E${ep.episode}", s.cover, "BR", listOf("series"), url))
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            SPText("Temporada ${ep.season} • Episódio ${ep.episode}", SPType.Label)
                            SPText(ep.title, SPType.Body)
                        }
                    }
                }
            }
        } else if (tab == LibraryTab.MOVIES) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(movies, key = { it.id }) { item ->
                    SPFocusIndicator(onClick = {
                        if (account != null) scope.launch {
                            try {
                                val detail = api.movieInfo(account, item.id)
                                val ext = detail?.extension ?: item.extension
                                val url = api.playbackUrl(account, "movie", item.id, ext)
                                onPlay(LiveChannel("xtream-movie:${item.id}", item.name, detail?.poster ?: item.poster, "BR", listOf("movies"), url))
                            } catch (e: Exception) { message = e.message ?: "Falha ao abrir filme." }
                        }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SPRemoteImage(item.poster, Modifier.width(58.dp).height(82.dp))
                            Column(Modifier.weight(1f)) {
                                SPText(item.name, SPType.Label, maxLines = 2)
                                SPText("Filme • abrir reprodução", SPType.Caption, color = SPColors.TextSecondary)
                                item.rating?.let { SPText("Nota: $it", SPType.Caption) }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(series, key = { it.id }) { item ->
                    SPFocusIndicator(onClick = {
                        if (account != null) scope.launch {
                            loading = true
                            try {
                                selectedSeries = item
                                episodes = api.seriesInfo(account, item.id)
                                message = if (episodes.isEmpty()) "Nenhum episódio retornado pelo provedor." else ""
                            } catch (e: Exception) {
                                selectedSeries = null
                                message = e.message ?: "Falha ao carregar episódios."
                            } finally { loading = false }
                        }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SPRemoteImage(item.cover, Modifier.width(58.dp).height(82.dp))
                            Column(Modifier.weight(1f)) {
                                SPText(item.name, SPType.Label, maxLines = 2)
                                SPText("Série • ver temporadas e episódios", SPType.Caption, color = SPColors.TextSecondary)
                                item.rating?.let { SPText("Nota: $it", SPType.Caption) }
                            }
                        }
                    }
                }
            }
        }
    }
}
