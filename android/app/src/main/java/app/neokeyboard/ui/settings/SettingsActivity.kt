package app.neokeyboard.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.neokeyboard.data.KeyboardSettings
import app.neokeyboard.ui.NeoTheme

/**
 * Màn cài đặt bàn phím. Cùng tiến trình với IME nên ghi prefs ở đây là StateFlow đổi,
 * Compose trong service tự recompose. Trạng thái bật/chọn đọc lại ở onResume vì người dùng
 * rời sang app Cài đặt hệ thống rồi quay lại.
 */
class SettingsActivity : ComponentActivity() {
    private lateinit var settings: KeyboardSettings
    private var imeEnabled by mutableStateOf(false)
    private var imeSelected by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settings = KeyboardSettings(this)
        setContent {
            val prefs by settings.state.collectAsState()
            NeoTheme(mode = prefs.mode, colorSource = prefs.colorSource, seed = prefs.customSeed) {
                SettingsScreen(
                    prefs = prefs,
                    settings = settings,
                    imeEnabled = imeEnabled,
                    imeSelected = imeSelected,
                    onOpenImeSettings = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                    onShowPicker = { getSystemService(InputMethodManager::class.java).showInputMethodPicker() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        imeEnabled = isNeoEnabled(this)
        imeSelected = isNeoSelected(this)
    }

    override fun onDestroy() {
        if (::settings.isInitialized) settings.close()
        super.onDestroy()
    }
}