package br.com.streetplay.data

import android.content.Context
import br.com.streetplay.data.remote.DiskCache
import br.com.streetplay.data.remote.Http
import br.com.streetplay.data.remote.M3uParser
import br.com.streetplay.data.remote.TmdbParser
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.LiveSource
import br.com.streetplay.domain.TrendingItem
import java.io.File

/** Fontes públicas: TMDB (destaques) e IPTV-org (canais). Funciona offline com o último cache salvo. */
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

    suspend fun channels(source: LiveSource): List<LiveChannel> {
        val text = cache.cached("live_${source.name}", TWELVE_HOURS) { Http.get(source.url) } ?: return emptyList()
        return runCatching { M3uParser.parse(text) }.getOrDefault(emptyList())
    }

    /** Resultado da última checagem de canais (válido por 2 h): url -> no ar? */
    suspend fun loadStatus(source: LiveSource): Map<String, Boolean> =
        cache.readFresh("status_${source.name}", TWO_HOURS)?.lineSequence()
            ?.mapNotNull { l -> if (l.length > 2 && (l[0] == '1' || l[0] == '0') && l[1] == '|') l.substring(2) to (l[0] == '1') else null }
            ?.toMap() ?: emptyMap()

    suspend fun saveStatus(source: LiveSource, status: Map<String, Boolean>) =
        cache.write("status_${source.name}", status.entries.joinToString("\n") { (if (it.value) "1|" else "0|") + it.key })

    private companion object {
        const val TWO_HOURS = 2 * 60 * 60 * 1000L
        const val SIX_HOURS = 6 * 60 * 60 * 1000L
        const val TWELVE_HOURS = 12 * 60 * 60 * 1000L
    }
}
