package br.com.streetplay.features.entertainment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import br.com.streetplay.StreetPlayApp
import br.com.streetplay.core.design.*
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.data.remote.XtreamEpisode
import br.com.streetplay.data.remote.XtreamSeriesItem
import br.com.streetplay.data.remote.XtreamVodItem
import kotlinx.coroutines.launch

private enum class LibraryTab { MOVIES, SERIES }

/** Experiência cinematográfica para filmes e séries, com foco/controle remoto e toque. */
@Composable
fun IptvLibraryScreen(onPlay: (LiveChannel) -> Unit, initialSeries: Boolean = false, modifier: Modifier = Modifier) {
    val mobileLayout = LocalConfiguration.current.screenWidthDp < 600
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as StreetPlayApp
    val account = remember { app.container.xtreamAccountStore.load() }
    val api = remember { app.container.xtreamApi }
    val scope = rememberCoroutineScope()
    var tab by remember(initialSeries) { mutableStateOf(if (initialSeries) LibraryTab.SERIES else LibraryTab.MOVIES) }
    var movies by remember { mutableStateOf<List<XtreamVodItem>>(emptyList()) }
    var series by remember { mutableStateOf<List<XtreamSeriesItem>>(emptyList()) }
    var episodes by remember { mutableStateOf<List<XtreamEpisode>>(emptyList()) }
    var selectedMovie by remember { mutableStateOf<XtreamVodItem?>(null) }
    var selectedSeries by remember { mutableStateOf<XtreamSeriesItem?>(null) }
    var selectedSeason by remember { mutableIntStateOf(1) }
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
            } catch (e: Exception) { message = e.message ?: "Não foi possível carregar o catálogo." }
            finally { loading = false }
        }
    }
    BackHandler(enabled = selectedMovie != null || selectedSeries != null) {
        if (selectedMovie != null) selectedMovie = null
        else { selectedSeries = null; episodes = emptyList() }
    }

    LaunchedEffect(tab, account) {
        selectedMovie = null; selectedSeries = null; episodes = emptyList(); refresh()
    }

    val dark = Color(0xFF080B10)
    val accent = LocalAccent.current
    Column(
        modifier.fillMaxSize().background(dark).padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LibrarySectionBanner(
            title = if (tab == LibraryTab.MOVIES) "FILMES" else "SÉRIES",
            subtitle = if (tab == LibraryTab.MOVIES) "Descubra e assista aos seus filmes" else "Suas séries, temporadas e episódios",
            onRefresh = { refresh() },
        )
        if (message.isNotBlank()) SPText(message, SPType.Caption, color = SPColors.TextSecondary)
        if (loading) SPText("Carregando catálogo…", SPType.Body, color = Color.White)

        if (selectedSeries != null) {
            val current = selectedSeries!!
            val seasons = episodes.map { it.season }.distinct().sorted()
            if (mobileLayout) {
                Column(Modifier.fillMaxSize().focusGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        SPRemoteImage(current.cover, Modifier.width(86.dp).height(118.dp).clip(RoundedCornerShape(12.dp)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SPButton("← Voltar", onClick = { selectedSeries = null; episodes = emptyList() })
                            SPText(current.name, SPType.Title, color = Color.White, maxLines = 3)
                            current.rating?.takeIf { it.isNotBlank() }?.let { SPText("Nota $it", SPType.Caption, color = SPColors.TextSecondary) }
                        }
                    }
                    SPText("Episódios", SPType.Title, color = Color.White)
                    if (seasons.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons) { season -> SPButton("Temporada $season", onClick = { selectedSeason = season }, selected = selectedSeason == season) }
                        }
                    }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(episodes.filter { it.season == (seasons.firstOrNull { v -> v == selectedSeason } ?: seasons.firstOrNull()) }, key = { it.id }) { ep ->
                            SPFocusIndicator(onClick = {
                                val url = api.playbackUrl(account!!, "series", ep.id, ep.extension ?: ep.container)
                                onPlay(LiveChannel("xtream-episode:${ep.id}", "${current.name} • T${ep.season} E${ep.episode}", current.cover, "BR", listOf("series"), url))
                            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    SPRemoteImage(current.cover, Modifier.width(76.dp).height(54.dp).clip(RoundedCornerShape(7.dp)))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        SPText("E${ep.episode} • ${ep.title}", SPType.Label, color = Color.White, maxLines = 2)
                                        SPText("Temporada ${ep.season}", SPType.Caption, color = SPColors.TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
            Row(Modifier.fillMaxSize().focusGroup(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Column(Modifier.weight(0.42f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SPButton("← Voltar às séries", onClick = { selectedSeries = null; episodes = emptyList() })
                    SPRemoteImage(current.cover, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)))
                    SPText(current.name, SPType.Title, color = Color.White, maxLines = 2)
                    current.rating?.takeIf { it.isNotBlank() }?.let { SPText("Nota $it", SPType.Caption, color = SPColors.TextSecondary) }
                    current.plot?.takeIf { it.isNotBlank() }?.let { SPText(it, SPType.Body, color = SPColors.TextSecondary, maxLines = 5) }
                }
                Column(Modifier.weight(0.58f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SPText("Episódios", SPType.Title, color = Color.White)
                    if (seasons.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons) { season ->
                                SPButton("Temporada $season", onClick = { selectedSeason = season }, selected = selectedSeason == season)
                            }
                        }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(episodes.filter { it.season == (seasons.firstOrNull { s -> s == selectedSeason } ?: seasons.firstOrNull()) }, key = { it.id }) { ep ->
                            SPFocusIndicator(
                                onClick = {
                                    val url = api.playbackUrl(account!!, "series", ep.id, ep.extension ?: ep.container)
                                    onPlay(LiveChannel("xtream-episode:${ep.id}", "${current.name} • T${ep.season} E${ep.episode}", current.cover, "BR", listOf("series"), url))
                                },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SPRemoteImage(current.cover, Modifier.width(112.dp).height(66.dp).clip(RoundedCornerShape(8.dp)))
                                    Column(Modifier.weight(1f)) {
                                        SPText("E${ep.episode} • ${ep.title}", SPType.Label, color = Color.White, maxLines = 2)
                                        SPText("Temporada ${ep.season}", SPType.Caption, color = SPColors.TextSecondary)
                                    }
                                    SPText("▶", SPType.Title, color = accent)
                                }
                            }
                        }
                    }
                }
            }
            }
        } else if (selectedMovie != null) {
            val movie = selectedMovie!!
            if (mobileLayout) {
                Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SPButton("← Voltar aos filmes", onClick = { selectedMovie = null })
                    SPRemoteImage(movie.poster, Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp)))
                    SPText(movie.name, SPType.Title, color = Color.White, maxLines = 3)
                    movie.rating?.takeIf { it.isNotBlank() }?.let { SPText("Nota $it", SPType.Label, color = accent) }
                    movie.plot?.takeIf { it.isNotBlank() }?.let { SPText(it, SPType.Body, color = SPColors.TextSecondary) }
                    SPButton("▶  Assistir agora", onClick = {
                        if (account != null) scope.launch {
                            loading = true
                            try {
                                val detail = runCatching { api.movieInfo(account, movie.id) }.getOrNull()
                                val url = api.playbackUrl(account, "movie", movie.id, detail?.extension ?: movie.extension)
                                onPlay(LiveChannel("xtream-movie:${movie.id}", movie.name, detail?.poster ?: movie.poster, "BR", listOf("movies"), url))
                            } catch (e: Exception) { message = e.message ?: "Falha ao abrir filme." }
                            finally { loading = false }
                        }
                    })
                }
            } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SPRemoteImage(movie.poster, Modifier.weight(0.42f).fillMaxHeight().clip(RoundedCornerShape(18.dp)))
                Column(Modifier.weight(0.58f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SPButton("← Voltar aos filmes", onClick = { selectedMovie = null })
                    SPText(movie.name, SPType.Display, color = Color.White, maxLines = 3)
                    movie.rating?.takeIf { it.isNotBlank() }?.let { SPText("Nota $it", SPType.Label, color = accent) }
                    movie.plot?.takeIf { it.isNotBlank() }?.let { SPText(it, SPType.Body, color = SPColors.TextSecondary, maxLines = 8) }
                    Spacer(Modifier.height(8.dp))
                    SPButton("▶  Assistir agora", onClick = {
                        if (account != null) scope.launch {
                            loading = true
                            try {
                                val detail = runCatching { api.movieInfo(account, movie.id) }.getOrNull()
                                val url = api.playbackUrl(account, "movie", movie.id, detail?.extension ?: movie.extension)
                                onPlay(LiveChannel("xtream-movie:${movie.id}", movie.name, detail?.poster ?: movie.poster, "BR", listOf("movies"), url))
                            } catch (e: Exception) { message = e.message ?: "Falha ao abrir filme." }
                            finally { loading = false }
                        }
                    })
                }
            }
            }
        } else if (tab == LibraryTab.MOVIES) {
            val hero = movies.firstOrNull()
            if (hero != null) {
                Box(Modifier.fillMaxWidth().height(224.dp).clip(RoundedCornerShape(18.dp))) {
                    SPRemoteImage(hero.poster, Modifier.fillMaxSize(), ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF0080B10), Color(0x99080B10), Color.Transparent))))
                    Column(Modifier.align(Alignment.CenterStart).widthIn(max = 430.dp).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SPText("DESTAQUE", SPType.Caption, color = accent)
                        SPText(hero.name, SPType.Display, color = Color.White, maxLines = 2)
                        SPText("Assista aos filmes disponíveis na sua assinatura", SPType.Body, color = Color(0xFFE0E0E0), maxLines = 2)
                        SPButton("Ver detalhes", onClick = { selectedMovie = hero })
                    }
                }
            }
            SPText("Continue assistindo", SPType.Title, color = Color.White)
            if (mobileLayout) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(movies, key = { it.id }) { item ->
                        SPFocusIndicator(onClick = { selectedMovie = item }, modifier = Modifier.fillMaxWidth().height(112.dp), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                SPRemoteImage(item.poster, Modifier.width(68.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp)))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    SPText(item.name, SPType.Label, color = Color.White, maxLines = 2)
                                    SPText("Filme", SPType.Caption, color = SPColors.TextSecondary)
                                }
                            }
                        }
                    }
                }
            } else {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(movies, key = { it.id }) { item ->
                    SPFocusIndicator(
                        onClick = { selectedMovie = item },
                        modifier = Modifier.width(164.dp).fillMaxHeight(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            SPRemoteImage(item.poster, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.height(7.dp))
                            SPText(item.name, SPType.Label, color = Color.White, maxLines = 2)
                            SPText("Filme", SPType.Caption, color = SPColors.TextSecondary)
                        }
                    }
                }
            }
            }
        } else {
            SPText("Séries para assistir", SPType.Title, color = Color.White)
            if (mobileLayout) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(series, key = { it.id }) { item ->
                        SPFocusIndicator(onClick = {
                            if (account != null) scope.launch {
                                loading = true
                                try {
                                    selectedSeries = item
                                    episodes = api.seriesInfo(account, item.id)
                                    selectedSeason = episodes.minOfOrNull { it.season } ?: 1
                                    message = if (episodes.isEmpty()) "Nenhum episódio retornado pelo provedor." else ""
                                } catch (e: Exception) {
                                    selectedSeries = null
                                    message = e.message ?: "Falha ao carregar episódios."
                                } finally { loading = false }
                            }
                        }, modifier = Modifier.fillMaxWidth().height(112.dp), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                SPRemoteImage(item.cover, Modifier.width(68.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp)))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    SPText(item.name, SPType.Label, color = Color.White, maxLines = 2)
                                    SPText("Série • temporadas e episódios", SPType.Caption, color = SPColors.TextSecondary, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            } else {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(series, key = { it.id }) { item ->
                    SPFocusIndicator(
                        onClick = {
                            if (account != null) scope.launch {
                                loading = true
                                try {
                                    selectedSeries = item
                                    episodes = api.seriesInfo(account, item.id)
                                    selectedSeason = episodes.minOfOrNull { it.season } ?: 1
                                    message = if (episodes.isEmpty()) "Nenhum episódio retornado pelo provedor." else ""
                                } catch (e: Exception) {
                                    selectedSeries = null
                                    message = e.message ?: "Falha ao carregar episódios."
                                } finally { loading = false }
                            }
                        },
                        modifier = Modifier.width(178.dp).fillMaxHeight(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            SPRemoteImage(item.cover, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(10.dp)))
                            Spacer(Modifier.height(7.dp))
                            SPText(item.name, SPType.Label, color = Color.White, maxLines = 2)
                            SPText("Série • temporadas e episódios", SPType.Caption, color = SPColors.TextSecondary, maxLines = 1)
                        }
                    }
                }
            }
            }
        }
        if (message.isNotBlank() && !loading) SPText(message, SPType.Caption, color = SPColors.TextSecondary)
    }
}


@Composable
private fun LibrarySectionBanner(title: String, subtitle: String, onRefresh: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .height(104.dp)
            .background(
                Brush.horizontalGradient(listOf(Color(0xFFFF8A00), Color(0xFFFF5A00), Color(0xFFD83B00))),
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(52.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SPText("▶", SPType.Title, color = Color.White)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SPText("STREET PLAY", SPType.Caption, color = Color.White.copy(alpha = 0.9f))
            SPText(title, SPType.Title, color = Color.White, maxLines = 1)
            SPText(subtitle, SPType.Caption, color = Color.White.copy(alpha = 0.92f), maxLines = 1)
        }
        SPButton("Atualizar", onClick = onRefresh)
    }
}
