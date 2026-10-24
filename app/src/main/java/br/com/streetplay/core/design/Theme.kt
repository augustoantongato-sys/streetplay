package br.com.streetplay.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAccent = staticCompositionLocalOf { SPColors.Orange }
val LocalAnimations = staticCompositionLocalOf { true }

@Composable
fun SPTheme(accentIndex: Int, animations: Boolean, content: @Composable () -> Unit) {
    val accent: Color = SPAccents[accentIndex.coerceIn(SPAccents.indices)].second
    CompositionLocalProvider(LocalAccent provides accent, LocalAnimations provides animations, content = content)
}
