package br.com.streetplay.core.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/** Carregador assíncrono de ícones (com cache LRU no repositório). */
val LocalIconLoader = staticCompositionLocalOf<suspend (String) -> ImageBitmap?> { { null } }
