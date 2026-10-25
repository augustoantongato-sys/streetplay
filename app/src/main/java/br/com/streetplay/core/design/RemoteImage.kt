package br.com.streetplay.core.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

val LocalRemoteImage = staticCompositionLocalOf<suspend (String) -> ImageBitmap?> { { null } }

@Composable
fun SPRemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable BoxScope.() -> Unit = {},
) {
    val loader = LocalRemoteImage.current
    val img by produceState<ImageBitmap?>(null, url) { value = if (url.isNullOrBlank()) null else loader(url) }
    Box(modifier) {
        val b = img
        if (b != null) Image(bitmap = b, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = contentScale)
        else placeholder()
    }
}
