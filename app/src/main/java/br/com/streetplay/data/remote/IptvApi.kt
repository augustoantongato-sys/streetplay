package br.com.streetplay.data.remote

import android.util.JsonReader
import android.util.JsonToken
import java.io.InputStream
import java.io.InputStreamReader

/** API completa do IPTV-org (https://github.com/iptv-org/api). Lida com os arquivos grandes em fluxo, sem estourar a memória. */
object IptvApi {
    private const val BASE = "https://iptv-org.github.io/api/"

    suspend fun streams(): List<RawStream> = Http.open(BASE + "streams.json") { input ->
        readArray(input) { r ->
            var channel: String? = null; var url: String? = null; var ref: String? = null
            var ua: String? = null; var quality: String? = null; var labels = emptyList<String>()
            r.beginObject()
            while (r.hasNext()) when (r.nextName()) {
                "channel" -> channel = r.stringOrNull()
                "url" -> url = r.stringOrNull()
                "referrer" -> ref = r.stringOrNull()
                "user_agent" -> ua = r.stringOrNull()
                "quality" -> quality = r.stringOrNull()
                "labels" -> labels = r.stringList()
                else -> r.skipValue()
            }
            r.endObject()
            if (channel != null && url != null && (url.startsWith("http://") || url.startsWith("https://")))
                RawStream(channel, url, ref, ua, quality, labels) else null
        }
    }

    suspend fun channels(keep: Set<String>): List<RawChannel> = Http.open(BASE + "channels.json") { input ->
        readArray(input) { r ->
            var id: String? = null; var name: String? = null; var country = ""
            var cats = emptyList<String>(); var nsfw = false; var closed = false; var network: String? = null
            r.beginObject()
            while (r.hasNext()) when (r.nextName()) {
                "id" -> id = r.stringOrNull()
                "name" -> name = r.stringOrNull()
                "country" -> country = r.stringOrNull().orEmpty()
                "categories" -> cats = r.stringList()
                "network" -> network = r.stringOrNull()
                "is_nsfw" -> nsfw = r.boolOrFalse()
                "closed" -> closed = !r.stringOrNull().isNullOrBlank()
                else -> r.skipValue()
            }
            r.endObject()
            if (id != null && name != null && id in keep) RawChannel(id, name, country, cats, nsfw, closed, network) else null
        }
    }

    suspend fun logos(keep: Set<String>): List<RawLogo> = Http.open(BASE + "logos.json") { input ->
        readArray(input) { r ->
            var channel: String? = null; var url: String? = null; var inUse = false; var format: String? = null; var width = 0
            r.beginObject()
            while (r.hasNext()) when (r.nextName()) {
                "channel" -> channel = r.stringOrNull()
                "url" -> url = r.stringOrNull()
                "in_use" -> inUse = r.boolOrFalse()
                "format" -> format = r.stringOrNull()
                "width" -> width = if (r.peek() == JsonToken.NULL) { r.nextNull(); 0 } else r.nextDouble().toInt()
                else -> r.skipValue()
            }
            r.endObject()
            if (channel != null && url != null && channel in keep) RawLogo(channel, url, inUse, format, width) else null
        }
    }

    /** Lista curada Free-TV/IPTV (segunda fonte). */
    suspend fun freeTv(): List<FreeTvEntry> =
        FreeTvParser.parse(Http.get("https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"))

    /** Canais removidos por pedido de direitos autorais ou conteúdo adulto: nunca listamos. */
    suspend fun blocklist(): Set<String> = Http.open(BASE + "blocklist.json") { input ->
        readArray(input) { r ->
            var channel: String? = null
            r.beginObject()
            while (r.hasNext()) if (r.nextName() == "channel") channel = r.stringOrNull() else r.skipValue()
            r.endObject()
            channel
        }.toSet()
    }

    private fun <T> readArray(input: InputStream, item: (JsonReader) -> T?): List<T> {
        val out = ArrayList<T>()
        JsonReader(InputStreamReader(input, Charsets.UTF_8).buffered(64 * 1024)).use { r ->
            r.beginArray()
            while (r.hasNext()) item(r)?.let { out += it }
            r.endArray()
        }
        return out
    }

    private fun JsonReader.stringOrNull(): String? =
        if (peek() == JsonToken.NULL) { nextNull(); null } else nextString()

    private fun JsonReader.boolOrFalse(): Boolean =
        if (peek() == JsonToken.BOOLEAN) nextBoolean() else { skipValue(); false }

    private fun JsonReader.stringList(): List<String> {
        if (peek() == JsonToken.NULL) { nextNull(); return emptyList() }
        val l = ArrayList<String>()
        beginArray()
        while (hasNext()) l += nextString()
        endArray()
        return l
    }
}
