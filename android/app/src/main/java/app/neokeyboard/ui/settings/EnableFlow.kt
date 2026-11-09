package app.neokeyboard.ui.settings

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.neokeyboard.ime.NeoImeService
import app.neokeyboard.ui.design.ZeroSpace

/**
 * Trạng thái IME (gọi lại ở onResume vì người dùng đổi trong app Cài đặt).
 * Android 14 chặn đọc Settings.Secure.ENABLED_INPUT_METHODS với targetSdk ≥ 34,
 * nên dùng InputMethodManager.enabledInputMethodList — API chính thức, không bị chặn.
 */
fun isNeoEnabled(context: Context): Boolean {
    val imm = context.getSystemService(InputMethodManager::class.java) ?: return false
    return imm.enabledInputMethodList.any { it.packageName == context.packageName }
}

fun isNeoSelected(context: Context): Boolean {
    val default = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    }.getOrNull() ?: ""
    return default.substringBefore('/') == context.packageName
}

fun neoImeId(context: Context): String =
    ComponentName(context, NeoImeService::class.java).flattenToShortString()

/**
 * Luồng bật bàn phím: hiện trạng thái + nút mở cài đặt IME và nút chọn bàn phím. Không thể
 * tự bật IME hộ người dùng (đặc quyền hệ thống) — chỉ dẫn tới đúng màn hình.
 */
@Composable
fun EnableFlow(
    enabled: Boolean,
    selected: Boolean,
    onOpenSettings: () -> Unit,
    onShowPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(ZeroSpace.Pad), verticalArrangement = Arrangement.spacedBy(ZeroSpace.Row)) {
        Text("Bắt đầu", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = when {
                !enabled -> "Chưa bật. Mở Cài đặt nhập liệu để bật Neo Keyboard, rồi chọn nó làm bàn phím."
                !selected -> "Đã bật nhưng chưa chọn. Chọn Neo Keyboard làm bàn phím hiện tại."
                else -> "Đang dùng Neo Keyboard. Mở một ô nhập bất kỳ để gõ thử."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Row)) {
            Button(onClick = onOpenSettings) { Text("Mở cài đặt") }
            Button(onClick = onShowPicker, enabled = enabled) { Text("Chọn bàn phím") }
        }
    }
}