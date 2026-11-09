package app.neokeyboard.ime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Trạng thái shift: tắt / một lần (chữ kế tiếp hoa) / khoá (caps lock). */
enum class ShiftMode { OFF, ONCE, LOCK }

/** Trang bố cục hiện tại. */
enum class KeyboardPage { LETTERS, SYMBOLS }

/**
 * State Compose của bàn phím. Service/controller mutate, KeyboardScreen đọc. Giữ một instance
 * suốt vòng đời service để không dựng lại ComposeView khi đổi field.
 */
class KeyboardUiState {
    var shiftMode by mutableStateOf(ShiftMode.OFF)
    var page by mutableStateOf(KeyboardPage.LETTERS)
    var composing by mutableStateOf("")
    var candidates by mutableStateOf<List<String>>(emptyList())
    var passwordField by mutableStateOf(false)
    var clipboardOpen by mutableStateOf(false)

    /** Dải chọn trigger (giữ phím trigger) đang mở trên dải gợi ý. */
    var triggerMenuOpen by mutableStateOf(false)
    var triggerItems by mutableStateOf<List<TriggerChip>>(emptyList())

    /** Tăng để ép NeoTheme đọc lại dynamic scheme khi màu hệ thống/ảnh nền đổi. */
    var paletteRevision by mutableIntStateOf(0)

    val shifted: Boolean get() = shiftMode != ShiftMode.OFF
}

/** Mục hiển thị trong dải chọn trigger. Không mang giá trị — bí mật không bao giờ lên UI. */
data class TriggerChip(val id: String, val label: String, val secret: Boolean, val active: Boolean)
