package app.neokeyboard.ui.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/** Icon phím chức năng vẽ bằng Canvas để khỏi kéo thư viện icon. Toạ độ chuẩn trong ô 24. */
enum class KeyGlyph { SHIFT, SHIFT_LOCK, BACKSPACE, ENTER, CLIPBOARD, BOLT, SETTINGS, LOCK, CLOSE }

@Composable
fun KeyIcon(glyph: KeyGlyph, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        val ox = (size.width - 24f * s) / 2f
        val oy = (size.height - 24f * s) / 2f
        translate(ox, oy) {
            scale(s, s, pivot = Offset.Zero) { drawGlyph(glyph, tint) }
        }
    }
}

private fun DrawScope.drawGlyph(glyph: KeyGlyph, tint: Color) {
    val stroke = Stroke(width = 2f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
    when (glyph) {
        KeyGlyph.SHIFT, KeyGlyph.SHIFT_LOCK -> {
            val arrow = Path().apply {
                moveTo(12f, 3f); lineTo(4f, 11f); lineTo(8.5f, 11f)
                lineTo(8.5f, 16f); lineTo(15.5f, 16f); lineTo(15.5f, 11f)
                lineTo(20f, 11f); close()
            }
            drawPath(arrow, tint, style = Fill)
            if (glyph == KeyGlyph.SHIFT_LOCK) {
                drawLine(tint, Offset(8.5f, 20f), Offset(15.5f, 20f), strokeWidth = 2.4f)
            }
        }
        KeyGlyph.BACKSPACE -> {
            val tag = Path().apply {
                moveTo(8f, 4f); lineTo(21f, 4f); lineTo(21f, 20f)
                lineTo(8f, 20f); lineTo(2f, 12f); close()
            }
            drawPath(tag, tint, style = stroke)
            drawLine(tint, Offset(11f, 9f), Offset(17f, 15f), strokeWidth = 2f)
            drawLine(tint, Offset(17f, 9f), Offset(11f, 15f), strokeWidth = 2f)
        }
        KeyGlyph.ENTER -> {
            val hook = Path().apply {
                moveTo(20f, 5f); lineTo(20f, 13f); lineTo(6f, 13f)
            }
            drawPath(hook, tint, style = stroke)
            val head = Path().apply {
                moveTo(10f, 8f); lineTo(5f, 13f); lineTo(10f, 18f)
            }
            drawPath(head, tint, style = stroke)
        }
        KeyGlyph.CLIPBOARD -> {
            drawRoundRect(tint, topLeft = Offset(5f, 4f), size = androidx.compose.ui.geometry.Size(14f, 16f), style = stroke)
            drawRoundRect(tint, topLeft = Offset(9f, 2f), size = androidx.compose.ui.geometry.Size(6f, 4f), style = Fill)
            drawLine(tint, Offset(8f, 10f), Offset(16f, 10f), strokeWidth = 1.6f)
            drawLine(tint, Offset(8f, 14f), Offset(14f, 14f), strokeWidth = 1.6f)
        }
        KeyGlyph.BOLT -> {
            val bolt = Path().apply {
                moveTo(13.5f, 2f); lineTo(5f, 13.5f); lineTo(11f, 13.5f)
                lineTo(10f, 22f); lineTo(19f, 10f); lineTo(13f, 10f); close()
            }
            drawPath(bolt, tint, style = Fill)
        }
        KeyGlyph.SETTINGS -> {
            val c = Offset(12f, 12f)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val dx = kotlin.math.cos(a).toFloat()
                val dy = kotlin.math.sin(a).toFloat()
                drawLine(
                    tint, Offset(12f + dx * 6.5f, 12f + dy * 6.5f), Offset(12f + dx * 9.5f, 12f + dy * 9.5f),
                    strokeWidth = 3.2f, cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
            drawCircle(tint, radius = 6.8f, center = c, style = Stroke(width = 2.4f))
            drawCircle(tint, radius = 2.4f, center = c, style = Stroke(width = 2f))
        }
        KeyGlyph.LOCK -> {
            drawRoundRect(
                tint, topLeft = Offset(5f, 11f), size = androidx.compose.ui.geometry.Size(14f, 10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f), style = Fill,
            )
            val shackle = Path().apply {
                moveTo(8f, 11f); lineTo(8f, 8f)
                cubicTo(8f, 3f, 16f, 3f, 16f, 8f); lineTo(16f, 11f)
            }
            drawPath(shackle, tint, style = stroke)
        }
        KeyGlyph.CLOSE -> {
            drawLine(tint, Offset(6f, 6f), Offset(18f, 18f), strokeWidth = 2f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(tint, Offset(18f, 6f), Offset(6f, 18f), strokeWidth = 2f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}