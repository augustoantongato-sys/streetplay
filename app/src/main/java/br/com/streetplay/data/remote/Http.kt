package br.com.streetplay.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object Http {
    // Catálogos Xtream de filmes/séries podem ultrapassar 8 MB em contas com muitos títulos.
    private const val DEFAULT_MAX_RESPONSE_BYTES = 64 * 1024 * 1024

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String =
        String(getBytes(url, headers), Charsets.UTF_8)

    suspend fun getBytes(
        url: String,
        headers: Map<String, String> = emptyMap(),
        maxBytes: Int = DEFAULT_MAX_RESPONSE_BYTES
    ): ByteArray = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15_000
            c.readTimeout = 45_000
            c.instanceFollowRedirects = true
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")

            val declaredLength = c.contentLengthLong
            if (declaredLength > maxBytes) {
                error("O catálogo enviado pelo provedor ultrapassa o limite de leitura de ${maxBytes / (1024 * 1024)} MB.")
            }

            c.inputStream.use { input ->
                readLimited(input, maxBytes)
            }
        } finally {
            c.disconnect()
        }
    }

    private fun readLimited(input: InputStream, maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(32 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) {
                error("O catálogo recebido ultrapassou o limite seguro de ${maxBytes / (1024 * 1024)} MB.")
            }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    /** Abre a resposta como fluxo (para arquivos grandes). O Android já descompacta gzip sozinho. */
    suspend fun <T> open(url: String, headers: Map<String, String> = emptyMap(), block: (InputStream) -> T): T =
        withContext(Dispatchers.IO) {
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 15_000
                c.readTimeout = 45_000
                c.instanceFollowRedirects = true
                headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
                if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
                c.inputStream.use(block)
            } finally {
                c.disconnect()
            }
        }
}
