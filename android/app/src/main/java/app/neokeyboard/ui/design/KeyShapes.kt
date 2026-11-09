package app.neokeyboard.ui.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * G-board parity: đo ~5.6dp. idle Pressed (8) / checked Field (12) / pressed Seam (4).
 * Nhấn thì bóp góc, không đổi kích thước.
 */
@Composable
fun zeroPressRadius(
    pressed: Boolean,
    checked: Boolean = false,
    motion: ZeroMotion = LocalZeroMotion.current,
): Dp = animateDpAsState(
    targetValue = when {
        pressed -> ZeroRadius.Seam
        checked -> ZeroRadius.Field
        else -> ZeroRadius.Pressed
    },
    animationSpec = motion.effects(ZeroDuration.Press),
    label = "zero-press-radius",
).value