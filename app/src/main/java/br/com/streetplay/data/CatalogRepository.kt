package br.com.streetplay.data

import android.content.Context
import br.com.streetplay.data.remote.DiskCache
import br.com.streetplay.data.remote.Http
import br.com.streetplay.data.remote.TmdbParser
import br.com.streetplay.domain.LiveChannel
import br.com.streetplay.domain.TrendingItem
import br.com.streetplay.domain.StreamingProvider
import java.io.File

/** TMDB fornece destaques e metadados de disponibilidade por serviço; o catálogo IPTV vem da conta Xtream do usuário. */
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

    /** Lista dinâmica de serviços de streaming que o TMDB informa para o Brasil. */
    suspend fun watchProviders(type: String = "movie", forceRefresh: Boolean = false): List<StreamingProvider> {
        if (tmdbToken.isBlank()) return emptyList()
        require(type == "movie" || type == "tv")
        val key = "watch_providers_br_$type"
        val fetch: suspend () -> String = {
            Http.get(
                "https://api.themoviedb.org/3/watch/providers/$type?watch_region=BR&language=pt-BR",
                mapOf("Authorization" to "Bearer $tmdbToken", "accept" to "application/json"),
            )
        }
        val json = (if (forceRefresh) {
            try { fetch().also { cache.write(key, it) } } catch (_: Exception) { cache.readAny(key) }
        } else cache.cached(key, SIX_HOURS, fetch)) ?: return emptyList()
        return runCatching { TmdbParser.parseWatchProviders(json) }.getOrDefault(emptyList())
    }

    /** Títulos que o TMDB associa a um serviço disponível no Brasil. Metadados apenas; reprodução depende do app oficial ou do IPTV do usuário. */
    suspend fun providerTitles(providerId: Int, type: String, forceRefresh: Boolean = false): List<TrendingItem> {
        if (tmdbToken.isBlank()) return emptyList()
        require(type == "movie" || type == "tv")
        val key = "provider_${providerId}_${type}"
        val fetch: suspend () -> String = {
            Http.get(
                "https://api.themoviedb.org/3/discover/$type?language=pt-BR&watch_region=BR&with_watch_providers=$providerId&with_watch_monetization_types=flatrate&sort_by=popularity.desc&page=1",
                mapOf("Authorization" to "Bearer $tmdbToken", "accept" to "application/json"),
            )
        }
        val json = (if (forceRefresh) runCatching { fetch().also { cache.write(key, it) } }.getOrElse { cache.readAny(key) }
        else cache.cached(key, SIX_HOURS, fetch)) ?: return emptyList()
        return runCatching { TmdbParser.parseDiscover(json, type) }.getOrDefault(emptyList())
    }

    /** Resultado da última checagem de canais (válido por 2 h): url -> no ar? */
    suspend fun loadStatus(): Map<String, Boolean> =
        cache.readFresh("status_all", TWO_HOURS)?.lineSequence()
            ?.mapNotNull { l -> if (l.length > 2 && (l[0] == '1' || l[0] == '0') && l[1] == '|') l.substring(2) to (l[0] == '1') else null }
            ?.toMap() ?: emptyMap()

    suspend fun saveStatus(status: Map<String, Boolean>) =
        cache.write("status_all", status.entries.joinToString("\n") { (if (it.value) "1|" else "0|") + it.key })

    private companion object {
        const val SIX_HOURS = 6 * 60 * 60 * 1000L
        const val TWO_HOURS = 2 * 60 * 60 * 1000L
    }
}
