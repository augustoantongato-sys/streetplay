package br.com.streetplay.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import br.com.streetplay.domain.AccentTheme
import br.com.streetplay.domain.AppMode

val LocalAccent = staticCompositionLocalOf { SPColors.Orange }
val LocalAccentTheme = staticCompositionLocalOf { AccentTheme.ORANGE }
val LocalAnimations = staticCompositionLocalOf { true }
val LocalLite = staticCompositionLocalOf { false }

/** Street Play = fundo escuro + a cor escolhida (6 opções). Lite = preto e branco, sem cor. */
@Composable
fun SPTheme(mode: AppMode, accent: AccentTheme, animations: Boolean, content: @Composable () -> Unit) {
    val lite = mode == AppMode.LITE
    CompositionLocalProvider(
        LocalAccent provides (if (lite) Color.White else Color(accent.argb)),
        LocalAccentTheme provides accent,
        LocalAnimations provides animations,
        LocalLite provides lite,
        content = content,
    )
}

/** O logo é laranja: no Lite fica branco; com outra cor de tema, assume a cor escolhida. */
@Composable
fun rememberLogoTint(): ColorFilter? {
    val lite = LocalLite.current
    val theme = LocalAccentTheme.current
    val accent = LocalAccent.current
    return when {
        lite -> ColorFilter.tint(Color.White)
        theme != AccentTheme.ORANGE -> ColorFilter.tint(accent)
        else -> null
    }
}
