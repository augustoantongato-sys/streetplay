package br.com.streetplay.data.remote

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import br.com.streetplay.domain.LiveChannel
import org.json.JSONArray
import org.json.JSONObject

data class XtreamCredentials(val server: String, val username: String, val password: String)

data class XtreamVodItem(
    val id: String, val name: String, val poster: String?, val categoryId: String?,
    val extension: String?, val rating: String?, val plot: String?,
)

data class XtreamSeriesItem(
    val id: String, val name: String, val cover: String?, val categoryId: String?,
    val rating: String?, val plot: String?,
)

data class XtreamEpisode(
    val id: String, val title: String, val season: Int, val episode: Int,
    val extension: String?, val container: String?,
)

/**
 * Cliente Xtream Codes para o catálogo contratado pelo próprio usuário.
 * Não contém listas públicas, credenciais padrão nem URLs de terceiros.
 */
class XtreamApi(context: Context) {
    private val responseCache = DiskCache(File(context.cacheDir, "xtream_catalog"))
    private val cacheAgeMs = 2 * 60 * 60 * 1000L
    suspend fun authenticate(credentials: XtreamCredentials): Boolean {
        val json = request(credentials)
        val user = json.optJSONObject("user_info") ?: error("Resposta inválida do provedor.")
        if (user.optInt("auth", 0) != 1) error("Usuário ou senha inválidos.")
        val status = user.optString("status")
        if (status.isNotBlank() && !status.equals("Active", true)) error("A assinatura IPTV não está ativa: $status")
        return true
    }

    suspend fun liveChannels(credentials: XtreamCredentials): List<LiveChannel> {
        val categories = categoryNames(requestArray(credentials, "get_live_categories"), "category_id", "category_name")
        val streams = requestArray(credentials, "get_live_streams")
        return buildList {
            for (i in 0 until streams.length()) {
                val item = streams.optJSONObject(i) ?: continue
                val id = item.optString("stream_id").takeIf { it.isNotBlank() && it != "null" } ?: continue
                val ext = item.optString("container_extension").ifBlank { "ts" }
                val categoryId = item.optString("category_id")
                val url = playbackUrl(credentials, "live", id, ext)
                add(LiveChannel(
                    id = "xtream:$id",
                    name = item.optString("name").ifBlank { "Canal $id" },
                    logo = item.optString("stream_icon").takeIf { it.startsWith("http") },
                    country = "BR",
                    categories = listOfNotNull(categories[categoryId]?.let { normalizeCategory(it) }),
                    url = url,
                    quality = null,
                    network = categories[categoryId],
                ))
            }
        }
    }

    suspend fun movies(credentials: XtreamCredentials): List<XtreamVodItem> {
        val json = requestArray(credentials, "get_vod_streams")
        return buildList {
            for (i in 0 until json.length()) {
                val item = json.optJSONObject(i) ?: continue
                val id = item.optString("stream_id")
                if (id.isBlank() || id == "null") continue
                add(XtreamVodItem(
                    id, item.optString("name"), item.optString("stream_icon").takeIf { it.startsWith("http") },
                    item.optString("category_id"), validExtension(item.optString("container_extension")),
                    item.optString("rating").takeIf { it.isNotBlank() }, null,
                ))
            }
        }
    }

    suspend fun movieInfo(credentials: XtreamCredentials, movieId: String): XtreamVodItem? {
        val root = request(credentials, "get_vod_info", "vod_id" to movieId)
        val info = root.optJSONObject("info") ?: JSONObject()
        val movie = root.optJSONObject("movie_data") ?: JSONObject()
        return XtreamVodItem(
            movieId, movie.optString("name"), info.optString("movie_image").takeIf { it.startsWith("http") },
            movie.optString("category_id"), validExtension(movie.optString("container_extension")),
            info.optString("rating"), info.optString("plot"),
        )
    }

    suspend fun series(credentials: XtreamCredentials): List<XtreamSeriesItem> {
        val json = requestArray(credentials, "get_series")
        return buildList {
            for (i in 0 until json.length()) {
                val item = json.optJSONObject(i) ?: continue
                val id = item.optString("series_id")
                if (id.isBlank() || id == "null") continue
                add(XtreamSeriesItem(
                    id, item.optString("name"), item.optString("cover").takeIf { it.startsWith("http") },
                    item.optString("category_id"), item.optString("rating"), item.optString("plot"),
                ))
            }
        }
    }

    suspend fun seriesInfo(credentials: XtreamCredentials, seriesId: String): List<XtreamEpisode> {
        val root = request(credentials, "get_series_info", "series_id" to seriesId)
        val episodes = root.optJSONObject("episodes") ?: return emptyList()
        val result = ArrayList<XtreamEpisode>()
        val seasons = episodes.keys()
        while (seasons.hasNext()) {
            val seasonKey = seasons.next()
            val seasonNumber = seasonKey.toIntOrNull() ?: 0
            val list = episodes.optJSONArray(seasonKey) ?: continue
            for (i in 0 until list.length()) {
                val ep = list.optJSONObject(i) ?: continue
                val id = ep.optString("id")
                if (id.isBlank() || id == "null") continue
                result += XtreamEpisode(
                    id, ep.optString("title").ifBlank { "Episódio ${i + 1}" },
                    ep.optInt("season", seasonNumber), ep.optInt("episode_num", i + 1),
                    validExtension(ep.optString("container_extension")),
                    validExtension(ep.optString("container_extension")),
                )
            }
        }
        return result.sortedWith(compareBy<XtreamEpisode> { it.season }.thenBy { it.episode })
    }

    fun playbackUrl(credentials: XtreamCredentials, type: String, id: String, extension: String?): String {
        val ext = extension?.takeIf { it.matches(Regex("[A-Za-z0-9]{2,5}")) } ?: if (type == "live") "ts" else "mp4"
        return "${credentials.server.trimEnd('/')}/$type/${Uri.encode(credentials.username)}/${Uri.encode(credentials.password)}/${Uri.encode(id)}.$ext"
    }

    private suspend fun requestArray(credentials: XtreamCredentials, action: String): JSONArray {
        val root = getJson(credentials, action)
        return when (root) {
            is JSONArray -> root
            is JSONObject -> {
                val detail = root.optString("error").takeIf { it.isNotBlank() && it != "null" }
                    ?: root.optString("message").takeIf { it.isNotBlank() && it != "null" }
                if (detail != null) error("Provedor IPTV: $detail")
                error("O provedor não retornou uma lista válida para $action. Confira se seu plano inclui esse catálogo.")
            }
            else -> error("Resposta inválida do provedor para $action.")
        }
    }

    private suspend fun request(credentials: XtreamCredentials, action: String? = null, vararg params: Pair<String, String>): JSONObject {
        val root = getJson(credentials, action, *params)
        return root as? JSONObject ?: error("Resposta inesperada do provedor.")
    }

    private suspend fun getJson(credentials: XtreamCredentials, action: String?, vararg params: Pair<String, String>): Any {
        val builder = Uri.parse(credentials.server.trimEnd('/') + "/player_api.php")
            .buildUpon()
            .appendQueryParameter("username", credentials.username)
            .appendQueryParameter("password", credentials.password)
        action?.let { builder.appendQueryParameter("action", it) }
        params.forEach { (key, value) -> builder.appendQueryParameter(key, value) }
        val requestUrl = builder.build().toString()
        val body = if (action == null) {
            Http.get(requestUrl)
        } else {
            val identity = credentials.server + "|" + credentials.username + "|" + credentials.password + "|" + action + "|" +
                params.joinToString("|") { it.first + "=" + it.second }
            val key = MessageDigest.getInstance("SHA-256")
                .digest(identity.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            responseCache.cached("xtream_" + key, cacheAgeMs) {
                Http.get(requestUrl).also { parseResponse(it) }
            } ?: Http.get(requestUrl)
        }
        return parseResponse(body)
    }

    private fun parseResponse(body: String): Any = try {
        if (body.trimStart().startsWith("[")) JSONArray(body) else JSONObject(body)
    } catch (_: Exception) {
        error("O provedor retornou uma resposta inválida. Verifique se o endereço informado é o servidor Xtream da sua conta, e não apenas o site do painel de revenda.")
    }

    private fun categoryNames(json: JSONArray, idKey: String, nameKey: String): Map<String, String> =
        buildMap {
            for (i in 0 until json.length()) {
                val item = json.optJSONObject(i) ?: continue
                val id = item.optString(idKey)
                val name = item.optString(nameKey)
                if (id.isNotBlank() && name.isNotBlank()) put(id, name)
            }
        }

    private fun normalizeCategory(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun validExtension(value: String?): String? =
        value?.takeIf { it.isNotBlank() && it != "null" && it.matches(Regex("[A-Za-z0-9]{2,5}")) }
}
