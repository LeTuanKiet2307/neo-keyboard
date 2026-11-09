package app.neokeyboard.ui.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * idle 16 / checked 12 / pressed 8 — nhấn thì bóp góc, không đổi kích thước.
 * Trích từ ZeroShape của System Zero ( chỉ phần cần cho phím).
 */
@Composable
fun zeroPressRadius(
    pressed: Boolean,
    checked: Boolean = false,
    motion: ZeroMotion = LocalZeroMotion.current,
): Dp = animateDpAsState(
    targetValue = when {
        pressed -> ZeroRadius.Pressed
        checked -> ZeroRadius.Field
        else -> ZeroRadius.Row
    },
    animationSpec = motion.effects(ZeroDuration.Press),
    label = "zero-press-radius",
).value