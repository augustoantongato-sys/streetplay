package br.com.streetplay.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class SPIconKind { HOME, APPS, STREAMING, FAVORITES, STORE, SETTINGS, INFO, WIFI, DOTS, TV }

/** Ícones desenhados em Canvas: sem dependência de fonte nem de pacote de ícones. */
@Composable
fun SPIcon(kind: SPIconKind, modifier: Modifier = Modifier, tint: Color = Color.White, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = s * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        fun poly(vararg pts: Pair<Float, Float>) = Path().apply {
            pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * s, y * s) else lineTo(x * s, y * s) }
            close()
        }
        when (kind) {
            SPIconKind.HOME -> drawPath(
                poly(.5f to .1f, .95f to .5f, .82f to .5f, .82f to .9f, .6f to .9f, .6f to .62f, .4f to .62f, .4f to .9f, .18f to .9f, .18f to .5f, .05f to .5f),
                tint,
            )
            SPIconKind.APPS -> listOf(.1f to .1f, .54f to .1f, .1f to .54f, .54f to .54f).forEach { (x, y) ->
                drawRoundRect(tint, p(x, y), Size(.36f * s, .36f * s), CornerRadius(.07f * s), style = stroke)
            }
            SPIconKind.STREAMING -> {
                drawCircle(tint, .42f * s, p(.5f, .5f), style = stroke)
                drawPath(poly(.42f to .31f, .42f to .69f, .72f to .5f), tint)
            }
            SPIconKind.FAVORITES -> drawPath(Path().apply {
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) .47f else .2f
                    val a = (-90 + 36 * i) * PI / 180
                    val x = (.5f + r * cos(a).toFloat()) * s
                    val y = (.53f + r * sin(a).toFloat()) * s
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }, tint)
            SPIconKind.STORE -> {
                drawRoundRect(tint, p(.16f, .32f), Size(.68f * s, .6f * s), CornerRadius(.07f * s), style = stroke)
                drawArc(tint, 180f, 180f, false, p(.34f, .1f), Size(.32f * s, .44f * s), style = stroke)
            }
            SPIconKind.SETTINGS -> {
                drawCircle(tint, .26f * s, p(.5f, .5f), style = stroke)
                drawCircle(tint, .09f * s, p(.5f, .5f), style = stroke)
                for (i in 0 until 8) {
                    val a = i * PI / 4
                    drawLine(tint, p(.5f + .34f * cos(a).toFloat(), .5f + .34f * sin(a).toFloat()),
                        p(.5f + .45f * cos(a).toFloat(), .5f + .45f * sin(a).toFloat()), strokeWidth = s * .1f, cap = StrokeCap.Round)
                }
            }
            SPIconKind.INFO -> {
                drawCircle(tint, .42f * s, p(.5f, .5f), style = stroke)
                drawCircle(tint, .055f * s, p(.5f, .3f))
                drawLine(tint, p(.5f, .45f), p(.5f, .72f), strokeWidth = s * .09f, cap = StrokeCap.Round)
            }
            SPIconKind.WIFI -> {
                listOf(.5f, .34f, .18f).forEach { r ->
                    drawArc(tint, 225f, 90f, false, p(.5f - r, .8f - r), Size(2 * r * s, 2 * r * s), style = stroke)
                }
                drawCircle(tint, .06f * s, p(.5f, .8f))
            }
            SPIconKind.TV -> {
                drawRoundRect(tint, p(.08f, .28f), Size(.84f * s, .6f * s), CornerRadius(.08f * s), style = stroke)
                drawLine(tint, p(.5f, .28f), p(.33f, .1f), strokeWidth = s * .08f, cap = StrokeCap.Round)
                drawLine(tint, p(.5f, .28f), p(.67f, .1f), strokeWidth = s * .08f, cap = StrokeCap.Round)
                drawPath(poly(.42f to .42f, .42f to .74f, .68f to .58f), tint)
            }
            SPIconKind.DOTS -> for (i in 0..2) for (j in 0..2) drawCircle(tint, .07f * s, p(.2f + .3f * i, .2f + .3f * j))
        }
    }
}
