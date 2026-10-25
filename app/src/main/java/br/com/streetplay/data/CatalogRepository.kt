package br.com.streetplay.data

import android.content.Context
import br.com.streetplay.data.remote.DiskCache
import br.com.streetplay.data.remote.Http
import br.com.streetplay.data.remote.IptvApi
import br.com.streetplay.data.remote.LiveIndexBuilder
import br.com.streetplay.data.remote.LiveIndexCodec
import br.com.streetplay.data.remote.TmdbParser
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.TrendingItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Fontes públicas: TMDB (destaques) e API completa do IPTV-org (canais). Funciona offline com o último cache salvo. */
class CatalogRepository(context: Context, private val tmdbToken: String) {
    private val cache = DiskCache(File(context.cacheDir, "catalog"))

    suspend fun trending(): List<TrendingItem> {
        if (tmdbToken.isBlank()) return emptyList()
        val json = cache.cached("trending_pt_br", SIX_HOURS) {
            Http.get(
                "https://api.themoviedb.org/3/trending/all/week?language=pt-BR",
                mapOf("Authorization" to "Bearer $tmdbToken", "accept" to "application/json"),
            )
        } ?: return emptyList()
        return runCatching { TmdbParser.parseTrending(json) }.getOrDefault(emptyList())
    }

    /** Lista completa de canais. Baixa e organiza uma vez por dia; depois abre na hora. */
    suspend fun liveChannels(onProgress: (String) -> Unit): List<LiveChannel> = withContext(Dispatchers.Default) {
        cache.readFresh(INDEX_KEY, ONE_DAY)?.let { return@withContext LiveIndexCodec.decode(it) }
        try {
            onProgress("Baixando a lista de canais (1/5)…")
            val streams = IptvApi.streams()
            onProgress("Baixando a lista de canais (2/5)…")
            val withStreams = streams.mapTo(HashSet()) { it.channel }
            val blocked = IptvApi.blocklist()
            val channels = IptvApi.channels(withStreams)
            onProgress("Baixando logos (3/5)…")
            val logos = IptvApi.logos(channels.mapTo(HashSet()) { it.id })
            onProgress("Baixando a segunda fonte (4/5)…")
            val extra = try { IptvApi.freeTv() } catch (e: CancellationException) { throw e } catch (e: Exception) { emptyList() }
            onProgress("Organizando (5/5)…")
            val list = LiveIndexBuilder.merge(LiveIndexBuilder.build(streams, channels, logos, blocked), extra, blocked)
            if (list.isNotEmpty()) cache.write(INDEX_KEY, LiveIndexCodec.encode(list))
            list
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cache.readAny(INDEX_KEY)?.let { LiveIndexCodec.decode(it) } ?: emptyList()
        }
    }

    /** Resultado da última checagem de canais (válido por 2 h): url -> no ar? */
    suspend fun loadStatus(): Map<String, Boolean> =
        cache.readFresh("status_all", TWO_HOURS)?.lineSequence()
            ?.mapNotNull { l -> if (l.length > 2 && (l[0] == '1' || l[0] == '0') && l[1] == '|') l.substring(2) to (l[0] == '1') else null }
            ?.toMap() ?: emptyMap()

    suspend fun saveStatus(status: Map<String, Boolean>) =
        cache.write("status_all", status.entries.joinToString("\n") { (if (it.value) "1|" else "0|") + it.key })

    private companion object {
        const val INDEX_KEY = "live_index_v5"
        const val SIX_HOURS = 6 * 60 * 60 * 1000L
        const val TWO_HOURS = 2 * 60 * 60 * 1000L
        const val ONE_DAY = 24 * 60 * 60 * 1000L
    }
}
