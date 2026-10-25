package br.com.streetplay.data.remote

import br.com.streetplay.domain.TrendingItem
import org.json.JSONObject

object TmdbParser {
    private const val IMG = "https://image.tmdb.org/t/p/"

    private fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key)

    /** Resposta de /trending/all/week. Ignora pessoas e conteúdo adulto. */
    fun parseTrending(json: String): List<TrendingItem> {
        val arr = JSONObject(json).optJSONArray("results") ?: return emptyList()
        val out = ArrayList<TrendingItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val type = o.str("media_type")
            if (type != "movie" && type != "tv") continue
            if (o.optBoolean("adult", false)) continue
            val title = o.str("title").ifBlank { o.str("name") }
            if (title.isBlank()) continue
            val date = o.str("release_date").ifBlank { o.str("first_air_date") }
            val poster = o.str("poster_path")
            val backdrop = o.str("backdrop_path")
            out += TrendingItem(
                id = o.optInt("id"),
                title = title,
                type = type,
                year = date.take(4),
                rating = o.optDouble("vote_average", 0.0),
                overview = o.str("overview"),
                posterUrl = if (poster.isBlank()) null else "${IMG}w342$poster",
                backdropUrl = if (backdrop.isBlank()) null else "${IMG}w780$backdrop",
            )
        }
        return out
    }
}
