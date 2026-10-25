package br.com.streetplay.data.remote

import br.com.streetplay.domain.LiveChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Testa se o endereço de um canal responde com vídeo/lista de reprodução (e não com erro ou página HTML). */
object StreamChecker {
    suspend fun isAlive(ch: LiveChannel): Boolean = withContext(Dispatchers.IO) {
        var c: HttpURLConnection? = null
        try {
            c = URL(ch.url).openConnection() as HttpURLConnection
            c.connectTimeout = 8_000
            c.readTimeout = 8_000
            c.instanceFollowRedirects = true
            c.setRequestProperty("User-Agent", ch.userAgent ?: "Mozilla/5.0 (Linux; Android 11; TV) StreetPlay")
            ch.referrer?.let { c.setRequestProperty("Referer", it) }
            c.setRequestProperty("Range", "bytes=0-2047")
            if (c.responseCode !in 200..299) return@withContext false
            val head = c.inputStream.use { s ->
                val buf = ByteArray(2048)
                val n = s.read(buf)
                if (n > 0) String(buf, 0, n, Charsets.ISO_8859_1) else ""
            }
            looksPlayable(c.contentType, head)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        } finally {
            c?.disconnect()
        }
    }

    fun looksPlayable(contentType: String?, head: String): Boolean {
        if (head.isEmpty()) return false
        val t = head.trimStart()
        return when {
            t.startsWith("#EXTM3U") -> true
            t.startsWith("<") -> false                         // página HTML/XML de erro
            contentType?.contains("html", true) == true -> false
            else -> true                                       // fluxo binário (ex.: MPEG-TS)
        }
    }
}
