package br.com.streetplay.core.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/** Carregador assíncrono de ícones (com cache LRU no repositório). */
/** Banner 16:9 do app (quando o app tem um). */
val LocalBannerLoader = staticCompositionLocalOf<suspend (String) -> ImageBitmap?> { { null } }

val LocalIconLoader = staticCompositionLocalOf<suspend (String) -> ImageBitmap?> { { null } }
