package app.neokeyboard.ui.keyboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.neokeyboard.ime.Key
import app.neokeyboard.ime.KeyboardUiState
import app.neokeyboard.ime.ShiftMode
import app.neokeyboard.ime.Special
import app.neokeyboard.ui.design.LocalZeroMotion
import app.neokeyboard.ui.design.ZeroDuration
import app.neokeyboard.ui.design.zeroContactShadow
import app.neokeyboard.ui.design.zeroPressRadius

/**
 * Phím nổi cơ học: đế tối (socket) + keycap sáng nổi lên trên, có bóng tiếp xúc cache một lần.
 * Nhấn → keycap chìm (wall 4→1dp), bóp góc 16→8, đổi màu nhấn; nhả → đàn hồi về. Không dùng
 * clickable để tách down/up (cần cho âm press/release). Bóng động mỗi frame bị tránh.
 */
@Composable
fun NeoKey(
    key: Key,
    ui: KeyboardUiState,
    popupEnabled: Boolean,
    onPress: (Key) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnRelease by rememberUpdatedState(onRelease)
    val motion = LocalZeroMotion.current
    val scheme = MaterialTheme.colorScheme

    val active = key.special == Special.SHIFT && ui.shifted
    val radius = zeroPressRadius(pressed, checked = active)
    val shape = RoundedCornerShape(radius)
    val wall by animateDpAsState(
        targetValue = if (pressed) 1.dp else 4.dp,
        animationSpec = motion.effects(ZeroDuration.Press),
        label = "key-travel",
    )

    val baseColor = scheme.surfaceContainerLow
    val capColor = when {
        pressed -> scheme.primaryContainer
        active -> scheme.secondaryContainer
        key.special != Special.NONE && key.special != Special.SPACE -> scheme.surfaceContainerHigh
        else -> scheme.surfaceContainerHighest
    }
    val contentColor = if (pressed) scheme.onPrimaryContainer else scheme.onSurface

    Box(
        modifier
            .pointerInput(key) {
                detectTapGestures(onPress = {
                    pressed = true
                    currentOnPress(key)
                    tryAwaitRelease()
                    pressed = false
                    currentOnRelease()
                })
            },
    ) {
        Box(Modifier.matchParentSize().background(baseColor, RoundedCornerShape(radius)))
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = wall)
                .zeroContactShadow(shape)
                .clip(shape)
                .background(capColor),
            contentAlignment = Alignment.Center,
        ) {
            KeyLabel(key, ui, contentColor)
        }
        if (pressed && popupEnabled && key.showPopup) {
            KeyPopup(displayText(key, ui))
        }
    }
}

@Composable
private fun KeyLabel(key: Key, ui: KeyboardUiState, color: Color) {
    when (key.special) {
        Special.SHIFT -> KeyIcon(
            if (ui.shiftMode == ShiftMode.LOCK) KeyGlyph.SHIFT_LOCK else KeyGlyph.SHIFT,
            color,
            Modifier.size(22.dp),
        )
        Special.BACKSPACE -> KeyIcon(KeyGlyph.BACKSPACE, color, Modifier.size(22.dp))
        Special.ENTER -> KeyIcon(KeyGlyph.ENTER, color, Modifier.size(22.dp))
        Special.SPACE -> Text(
            "space",
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.5f),
        )
        Special.TO_SYMBOLS, Special.TO_LETTERS -> Text(
            key.text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
        Special.NONE -> Text(
            displayText(key, ui),
            style = MaterialTheme.typography.titleLarge,
            color = color,
        )
    }
}

private fun displayText(key: Key, ui: KeyboardUiState): String =
    if (key.isLetter && ui.shifted) key.text.uppercase() else key.text