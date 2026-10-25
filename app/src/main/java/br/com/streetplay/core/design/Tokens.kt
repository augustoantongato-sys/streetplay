package br.com.streetplay.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SPColors {
    val Orange = Color(0xFFFF6A00)
    val OrangeSecondary = Color(0xFFFF8C32)
    val OrangeLight = Color(0xFFFFB15C)
    val Background = Color(0xFF101010)
    val BackgroundSecondary = Color(0xFF171717)
    val Card = Color(0xFF202020)
    val CardFocused = Color(0xFF2B2B2B)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA5A5A5)
    val Sidebar = Color(0xFF0C0C0C)
    val TileBase = Color(0xFF171717)
    val TileFocused = Color(0xFF262626)
    val Border = Color(0xFF2A2A2A)
}

object SPSpacing { val xs = 4.dp; val sm = 8.dp; val md = 16.dp; val lg = 24.dp; val xl = 40.dp }
object SPRadius { val sm = 8.dp; val md = 14.dp; val lg = 22.dp }
object SPMotion { const val Fast = 120; const val Normal = 250; const val FocusScale = 1.08f }

object SPType {
    val Display = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, color = SPColors.TextPrimary)
    val Title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = SPColors.TextPrimary)
    val Body = TextStyle(fontSize = 18.sp, color = SPColors.TextPrimary)
    val Label = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SPColors.TextPrimary)
    val Caption = TextStyle(fontSize = 13.sp, color = SPColors.TextSecondary)
}

/** Paleta do modo Lite: apenas preto, branco e cinzas. */
object LiteColors {
    val Tile = Color(0xFF1C1C1E)
    val TileFocused = Color(0xFF2C2C2E)
}
