package br.com.streetplay.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL

object Http {
    // Limite por resposta individual; não limita o tamanho total do catálogo.
    private const val DEFAULT_MAX_RESPONSE_BYTES = 64 * 1024 * 1024

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String =
        String(getBytes(url, headers), Charsets.UTF_8)

    suspend fun getBytes(
        url: String,
        headers: Map<String, String> = emptyMap(),
        maxBytes: Int = DEFAULT_MAX_RESPONSE_BYTES
    ): ByteArray = withContext(Dispatchers.IO) {
        val c = openConnection(url, headers)
        try {
            val code = c.responseCode
            if (code !in 200..299) throw httpError(c, url, code)

            val declaredLength = c.contentLengthLong
            if (declaredLength > maxBytes) {
                error("A resposta do provedor tem mais de ${maxBytes / (1024 * 1024)} MB. O limite é por resposta, não pelo catálogo inteiro.")
            }
            c.inputStream.use { input -> readLimited(input, maxBytes) }
        } catch (e: Exception) {
            throw explainConnectionError(e, url)
        } finally {
            c.disconnect()
        }
    }

    private fun openConnection(url: String, headers: Map<String, String>): HttpURLConnection {
        val parsed = try { URL(url) } catch (_: Exception) {
            error("Endereço do servidor inválido. Informe a URL completa, incluindo http:// ou https:// e a porta, se houver.")
        }
        require(parsed.protocol.equals("http", true) || parsed.protocol.equals("https", true)) {
            "Use um endereço HTTP ou HTTPS para o servidor IPTV."
        }
        return (parsed.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 45_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json, text/plain, */*")
            setRequestProperty("User-Agent", "StreetPlay/1.0 (Android)")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
    }

    private fun httpError(c: HttpURLConnection, url: String, code: Int): Exception {
        val endpoint = try { URL(url).path.ifBlank { "/" } } catch (_: Exception) { "endereço informado" }
        val detail = when (code) {
            404 -> "HTTP 404: o servidor respondeu, mas não encontrou o caminho $endpoint. Confira se o endereço é o servidor de acesso IPTV e se ele oferece a API Xtream em /player_api.php."
            401, 403 -> "HTTP $code: o servidor recusou a solicitação. Verifique usuário, senha, validade do acesso ou bloqueio de conexão."
            408, 504 -> "HTTP $code: o servidor demorou demais para responder. Tente novamente."
            429 -> "HTTP 429: o servidor limitou temporariamente as solicitações. Aguarde um pouco e tente novamente."
            in 500..599 -> "HTTP $code: o servidor IPTV apresentou uma falha interna. Isso não indica necessariamente que sua conta esteja inválida."
            else -> "HTTP $code ao consultar o servidor IPTV no caminho $endpoint."
        }
        return IllegalStateException(detail)
    }

    private fun explainConnectionError(error: Exception, url: String): Exception {
        if (error is IllegalStateException && error.message?.startsWith("HTTP ") == true) return error
        val host = try { URL(url).host } catch (_: Exception) { "servidor informado" }
        return when (error) {
            is SocketTimeoutException -> IllegalStateException("Tempo esgotado ao aguardar resposta de $host. O servidor pode estar lento ou bloqueando a conexão.")
            is UnknownHostException -> IllegalStateException("Não foi possível localizar o endereço $host. Confira o domínio e sua conexão com a internet.")
            is ConnectException -> IllegalStateException("Não foi possível abrir conexão com $host. Confira o endereço, a porta e se o servidor aceita conexões externas.")
            else -> error
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
                error("A resposta recebida ultrapassou o limite de ${maxBytes / (1024 * 1024)} MB por requisição.")
            }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    /** Abre respostas em fluxo para catálogos grandes. */
    suspend fun <T> open(url: String, headers: Map<String, String> = emptyMap(), block: (InputStream) -> T): T =
        withContext(Dispatchers.IO) {
            val c = openConnection(url, headers)
            try {
                val code = c.responseCode
                if (code !in 200..299) throw httpError(c, url, code)
                c.inputStream.use(block)
            } catch (e: Exception) {
                throw explainConnectionError(e, url)
            } finally {
                c.disconnect()
            }
        }
}
