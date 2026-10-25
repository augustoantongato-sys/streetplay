package br.com.streetplay.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object Http {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String =
        String(getBytes(url, headers), Charsets.UTF_8)

    suspend fun getBytes(url: String, headers: Map<String, String> = emptyMap(), maxBytes: Int = 8_000_000): ByteArray =
        withContext(Dispatchers.IO) {
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 10_000
                c.readTimeout = 15_000
                c.instanceFollowRedirects = true
                headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
                if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
                val bytes = c.inputStream.use { it.readBytes() }
                check(bytes.size <= maxBytes) { "Resposta grande demais" }
                bytes
            } finally {
                c.disconnect()
            }
        }
}
