package br.com.streetplay.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import br.com.streetplay.domain.AppMode

val LocalAccent = staticCompositionLocalOf { SPColors.Orange }
val LocalAnimations = staticCompositionLocalOf { true }
val LocalLite = staticCompositionLocalOf { false }

/** Street Play = preto + laranja da marca. Lite = preto e branco. As cores não são editáveis. */
@Composable
fun SPTheme(mode: AppMode, animations: Boolean, content: @Composable () -> Unit) {
    val lite = mode == AppMode.LITE
    CompositionLocalProvider(
        LocalAccent provides (if (lite) Color.White else SPColors.Orange),
        LocalAnimations provides animations,
        LocalLite provides lite,
        content = content,
    )
}
