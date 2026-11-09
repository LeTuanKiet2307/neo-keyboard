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
    private lateinit var triggers: app.neokeyboard.trigger.TriggerStore
    private var focusSection by mutableStateOf<String?>(null)
    /** Cùng bảng màu System Zero với bàn phím: đọc cache ngay, làm mới từ cầu ở onResume. */
    private var themeSnapshot by mutableStateOf<app.neokeyboard.theme.ThemeSnapshot?>(null)
    private var imeEnabled by mutableStateOf(false)
    private var imeSelected by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settings = KeyboardSettings(this)
        triggers = app.neokeyboard.trigger.TriggerStore(this)
        focusSection = intent?.getStringExtra(EXTRA_SECTION)
        themeSnapshot = runCatching { app.neokeyboard.theme.ThemeBridgeClient(this).cacheRead(this) }.getOrNull()
        setContent {
            val prefs by settings.state.collectAsState()
            val triggerState by triggers.state.collectAsState()
            NeoTheme(mode = prefs.mode, colorSource = prefs.colorSource, seed = prefs.customSeed, systemZero = themeSnapshot) {
                SettingsScreen(
                    prefs = prefs,
                    settings = settings,
                    imeEnabled = imeEnabled,
                    imeSelected = imeSelected,
                    onOpenImeSettings = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
                    onShowPicker = { getSystemService(InputMethodManager::class.java).showInputMethodPicker() },
                    triggerState = triggerState,
                    triggers = triggers,
                    focusSection = focusSection,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        focusSection = intent.getStringExtra(EXTRA_SECTION)
    }

    override fun onResume() {
        super.onResume()
        imeEnabled = isNeoEnabled(this)
        imeSelected = isNeoSelected(this)
        val context = applicationContext
        Thread {
            val fresh = runCatching { app.neokeyboard.theme.ThemeBridgeClient(context).loadSnapshot(context) }.getOrNull()
            if (fresh != null) runOnUiThread { themeSnapshot = fresh }
        }.start()
    }

    override fun onDestroy() {
        if (::settings.isInitialized) settings.close()
        if (::triggers.isInitialized) triggers.close()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SECTION = "section"
        const val SECTION_TRIGGER = "trigger"
    }
}