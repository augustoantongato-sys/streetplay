package br.com.streetplay.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Cache simples em arquivo: usa o dado salvo se estiver fresco; sem internet, usa o último salvo (offline). */
class DiskCache(private val dir: File) {
    init { dir.mkdirs() }

    private fun file(key: String) = File(dir, key.replace(Regex("[^A-Za-z0-9._-]"), "_"))

    suspend fun cached(key: String, maxAgeMs: Long, fetch: suspend () -> String): String? = withContext(Dispatchers.IO) {
        val f = file(key)
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < maxAgeMs) return@withContext f.readText()
        try {
            fetch().also { f.writeText(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (f.exists()) f.readText() else null
        }
    }
}
