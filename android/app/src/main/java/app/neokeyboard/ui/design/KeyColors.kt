package app.neokeyboard.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Suy màu keycap từ ColorScheme System Zero (SPEC §6.4): chỉ lerp sáng/tối, không hardcode. */
fun lighten(color: Color, amount: Float): Color = lerp(color, Color.White, amount.coerceIn(0f, 1f))

fun darken(color: Color, amount: Float): Color = lerp(color, Color.Black, amount.coerceIn(0f, 1f))

/** Socket: surfaceContainerLow tối 4%. */
fun socketColor(base: Color): Color = darken(base, 0.04f)

/** Side wall: surfaceContainerHigh tối 8%. */
fun wallColor(base: Color): Color = darken(base, 0.08f)

/** Cap gradient sáng 4% → gốc → tối 6%. */
fun capStops(base: Color): Triple<Color, Color, Color> =
    Triple(lighten(base, 0.04f), base, darken(base, 0.06f))

/** Wall khi nhấn: primaryContainer tối 12%. */
fun pressedWallColor(primaryContainer: Color): Color = darken(primaryContainer, 0.12f)

fun pressedCapStops(primaryContainer: Color): Triple<Color, Color, Color> =
    Triple(lighten(primaryContainer, 0.04f), primaryContainer, darken(primaryContainer, 0.06f))

object KeyColorTokens {
    val TopHighlight = Color.White.copy(alpha = 0.06f)
    val BottomInnerShadow = Color.Black.copy(alpha = 0.08f)
}
