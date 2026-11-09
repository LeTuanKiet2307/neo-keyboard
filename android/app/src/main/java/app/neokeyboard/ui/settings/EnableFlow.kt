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
