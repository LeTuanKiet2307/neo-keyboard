package app.neokeyboard.ui.keyboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.neokeyboard.ime.Key
import app.neokeyboard.ime.KeyboardUiState
import app.neokeyboard.ime.ShiftMode
import app.neokeyboard.ime.Special
import app.neokeyboard.ui.design.KeyColorTokens
import app.neokeyboard.ui.design.LocalZeroMotion
import app.neokeyboard.ui.design.ZeroDuration
import app.neokeyboard.ui.design.ZeroRadius
import app.neokeyboard.ui.design.ZeroSize
import app.neokeyboard.ui.design.capStops
import app.neokeyboard.ui.design.darken
import app.neokeyboard.ui.design.pressedCapStops
import app.neokeyboard.ui.design.pressedWallColor
import app.neokeyboard.ui.design.socketColor
import app.neokeyboard.ui.design.wallColor
import app.neokeyboard.ui.design.zeroContactShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keycap 3 lớp (SPEC §6, G-board parity): socket → side wall → cap top.
 * Cap cố định padding(bottom=wall), chỉ dịch qua graphicsLayer (travel 3dp)
 * để không layout lại mỗi frame. Bo/màu snap theo pressed, không animate.
 */
@Composable
fun NeoKey(
    key: Key,
    ui: KeyboardUiState,
    popupEnabled: Boolean,
    onKeyDown: (Key) -> Unit,
    onKeyUp: (Key, Boolean) -> Unit,
    onKeyCancel: (Key) -> Unit = {},
    onBackspaceRepeatStart: () -> Unit = {},
    onBackspaceWordDelete: () -> Unit = {},
    onTriggerLongPress: () -> Unit = {},
    inset: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    var pressed by remember { mutableStateOf(false) }
    var canceled by remember { mutableStateOf(false) }
    val currentOnKeyDown by rememberUpdatedState(onKeyDown)
    val currentOnKeyUp by rememberUpdatedState(onKeyUp)
    val currentOnKeyCancel by rememberUpdatedState(onKeyCancel)
    val currentOnBackspaceRepeatStart by rememberUpdatedState(onBackspaceRepeatStart)
    val currentOnBackspaceWordDelete by rememberUpdatedState(onBackspaceWordDelete)
    val currentOnTriggerLongPress by rememberUpdatedState(onTriggerLongPress)
    val motion = LocalZeroMotion.current
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var boundsSizePx by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    val active = key.special == Special.SHIFT && ui.shifted
    // Snap bo góc (SPEC §6.5): không animate shape mỗi frame.
    val radius = when {
        pressed -> ZeroRadius.Seam
        active -> ZeroRadius.Field
        else -> ZeroRadius.Pressed
    }
    val shape = RoundedCornerShape(radius)

    val progress = remember { Animatable(0f) }
    val travelPx = remember(density) { with(density) { ZeroSize.KeyTravel.toPx() } }

    val isSpace = key.special == Special.SPACE
    val isFunctional = key.special != Special.NONE && !isSpace
    // Space mang màu nhấn chủ đạo (primaryContainer) của bảng màu System Zero; nhấn thì đậm lên primary.
    val capBase = when {
        isSpace -> scheme.primaryContainer
        isFunctional -> scheme.surfaceContainerHigh
        else -> scheme.surfaceContainerHighest
    }
    val capPressedBase = when {
        isSpace -> scheme.primary
        isFunctional -> scheme.secondaryContainer
        else -> scheme.primaryContainer
    }
    val (capTop, capMid, capBottom) = if (pressed) pressedCapStops(capPressedBase) else capStops(capBase)
    val wallBase = if (pressed) pressedWallColor(capPressedBase) else wallColor(capBase)
    val socketBase = socketColor(scheme.surfaceContainerLow)
    val contentColor = when {
        isSpace && pressed -> scheme.onPrimary
        isSpace -> scheme.onPrimaryContainer
        pressed && isFunctional -> scheme.onSecondaryContainer
        pressed -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }

    val description = when (key.special) {
        Special.BACKSPACE -> "Xóa"
        Special.ENTER -> "Enter"
        Special.SHIFT -> "Shift"
        Special.TO_SYMBOLS -> "Bàn phím số"
        Special.TO_LETTERS -> "Bàn phím chữ"
        Special.SPACE -> "Dấu cách"
        Special.TRIGGER -> "Nhập nhanh (giữ để chọn)"
        Special.NONE -> displayText(key, ui)
    }

    Box(
        modifier
            .onGloballyPositioned { boundsSizePx = it.size }
            .pointerInput(key) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    canceled = false
                    currentOnKeyDown(key)
                    if (motion.enabled) {
                        scope.launch {
                            progress.animateTo(
                                1f,
                                spring(dampingRatio = 0.75f, stiffness = 900f),
                            )
                        }
                    } else scope.launch { progress.snapTo(1f) }
                    val downPos = down.position
                    val repeatJob = if (key.special == Special.BACKSPACE) {
                        scope.launch {
                            delay(300)
                            if (!canceled) currentOnBackspaceRepeatStart()
                        }
                    } else null
                    var longPressFired = false
                    val longPressJob = if (key.special == Special.TRIGGER) {
                        scope.launch {
                            delay(380)
                            if (!canceled) {
                                longPressFired = true
                                currentOnTriggerLongPress()
                            }
                        }
                    } else null
                    var upInside = false
                    var wordDeleteConsumed = false
                    val swipeThresholdPx = with(density) { 24.dp.toPx() }
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val inside = (change.position.x in 0f..boundsSizePx.width.toFloat() &&
                            change.position.y in 0f..boundsSizePx.height.toFloat())
                        if (!inside && !canceled) {
                            canceled = true
                            repeatJob?.cancel()
                            longPressJob?.cancel()
                            currentOnKeyCancel(key)
                        }
                        if (!wordDeleteConsumed && key.special == Special.BACKSPACE &&
                            change.position.x < downPos.x - swipeThresholdPx
                        ) {
                            wordDeleteConsumed = true
                            repeatJob?.cancel()
                            currentOnBackspaceWordDelete()
                        }
                        if (!change.pressed) {
                            upInside = inside
                            break
                        }
                    }
                    repeatJob?.cancel()
                    longPressJob?.cancel()
                    pressed = false
                    if (motion.enabled) {
                        scope.launch {
                            progress.animateTo(
                                0f,
                                tween(ZeroDuration.KeyTravel),
                            )
                        }
                    } else scope.launch { progress.snapTo(0f) }
                    // Giữ trigger đã mở dải chọn → thả tay không nhập thêm.
                    currentOnKeyUp(key, upInside && !canceled && !longPressFired)
                }
            }
            .semantics {
                role = Role.Button
                contentDescription = description
            },
    ) {
      // Ô ngoài = vùng chạm (phủ cả khe); keycap nhìn thấy lùi vào bằng [inset] và chỉ chiếm
      // visualWeight/weight chiều ngang (phần dư là vùng chạm mở rộng của 'a'/'l').
      Box(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(key.visualWeight / key.weight)
            .align(if (key.visualAlignEnd) Alignment.CenterEnd else Alignment.CenterStart)
            .padding(inset),
      ) {
        // 1. Socket (đế lõm)
        Box(Modifier.fillMaxSize().background(socketBase, shape))
        // 2. Side wall: cố định padding để lộ 4dp ở đáy; cap dịch xuống 3dp → còn 1dp.
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 1.dp, end = 1.dp, top = 1.dp, bottom = ZeroSize.KeyWall),
        ) {
            Box(Modifier.fillMaxSize().clip(shape).background(wallBase))
        }
        // 3. Cap top: chỉ đọc progress trong graphicsLayer.
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 1.dp, end = 1.dp, top = 1.dp, bottom = ZeroSize.KeyWall)
                .graphicsLayer { translationY = progress.value * travelPx }
                .zeroContactShadow(shape)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(capTop, capMid, capBottom),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(1.dp))
                KeyLabel(key, ui, contentColor)
                Spacer(Modifier.height(2.dp))
            }
        }
        // Viền sáng trên + bóng đáy cap (vẽ trong, không layout)
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 1.dp, end = 1.dp, top = 1.dp, bottom = ZeroSize.KeyWall)
                .graphicsLayer { translationY = progress.value * travelPx },
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(Modifier.height(1.dp).fillMaxSize().background(KeyColorTokens.TopHighlight))
        }
        if (pressed && !canceled && popupEnabled && key.showPopup) {
            KeyPopup(displayText(key, ui))
        }
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
            "Tiếng Việt",
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.8f),
            maxLines = 1,
        )
        Special.TRIGGER -> KeyIcon(KeyGlyph.BOLT, color, Modifier.size(20.dp))
        Special.TO_SYMBOLS, Special.TO_LETTERS -> Text(
            key.text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
        )
        Special.NONE -> Text(
            displayText(key, ui),
            style = MaterialTheme.typography.titleLarge,
            color = color,
            maxLines = 1,
        )
    }
}

private fun displayText(key: Key, ui: KeyboardUiState): String =
    if (key.isLetter && ui.shifted) key.text.uppercase() else key.text
