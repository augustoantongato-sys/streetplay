package br.com.streetplay.data.remote

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Carrega imagens da internet com cache em memória (24 MB) e em disco. Reduz imagens grandes para poupar RAM. */
class RemoteImageLoader(context: Context) {
    private val dir = File(context.cacheDir, "img").also { it.mkdirs() }
    private val memory = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    suspend fun load(url: String): ImageBitmap? {
        memory.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val file = File(dir, url.hashCode().toUInt().toString(16))
                val bytes = if (file.exists()) file.readBytes() else Http.getBytes(url, maxBytes = 3_000_000).also { file.writeBytes(it) }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 600) sample *= 2
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                bmp?.asImageBitmap()?.also { memory.put(url, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }
}
