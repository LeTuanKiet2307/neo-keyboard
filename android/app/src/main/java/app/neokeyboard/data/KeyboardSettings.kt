package app.neokeyboard.data

import android.content.Context
import android.content.SharedPreferences
import app.neokeyboard.ui.ColorSource
import app.neokeyboard.ui.design.ZeroPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Mức rung phản hồi phím. */
enum class HapticStrength(val label: String) { LIGHT("Nhẹ"), MEDIUM("Vừa"), STRONG("Mạnh") }

/** Ảnh chụp toàn bộ tuỳ chọn bàn phím; UI đọc từ đây, không đọc prefs trực tiếp. */
data class KeyboardPrefs(
    val mode: AppearanceMode = AppearanceMode.SYSTEM,
    val colorSource: ColorSource = ColorSource.SYSTEM,
    val customSeed: Int = ZeroPalette.DefaultSeed,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    val soundPack: String = "cream_linear",
    val hapticsEnabled: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    val telexEnabled: Boolean = true,
    val popupEnabled: Boolean = true,
    val suggestionsEnabled: Boolean = true,
    val learnWords: Boolean = true,
    val suggestionCount: Int = 4,
    val clipboardEnabled: Boolean = true,
    val clipboardDays: Int = 30,
    val autoCap: Boolean = true,
    val doubleSpacePeriod: Boolean = true,
    /** false (mặc định): gõ thẳng, không gạch chân từ đang gõ. true: composing chuẩn của Android. */
    val composingUnderline: Boolean = false,
)

/**
 * SharedPreferences → StateFlow. Service và Activity cùng tiến trình nên không cần IPC:
 * ghi ở SettingsScreen thì StateFlow đổi, Compose trong IME tự recompose.
 */
class KeyboardSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<KeyboardPrefs> = _state.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _state.value = read() }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun close() = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    fun setMode(value: AppearanceMode) = prefs.edit().putString(KEY_MODE, value.name).apply()
    fun setColorSource(value: ColorSource) = prefs.edit().putString(KEY_COLOR_SOURCE, value.name).apply()
    fun setCustomSeed(value: Int) = prefs.edit().putInt(KEY_SEED, value).apply()
    fun setSoundEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_SOUND, value).apply()
    fun setSoundVolume(value: Float) = prefs.edit().putFloat(KEY_VOLUME, value.coerceIn(0f, 1f)).apply()
    fun setSoundPack(value: String) = prefs.edit().putString(KEY_SOUND_PACK, value).apply()
    fun setHapticsEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()
    fun setHapticStrength(value: HapticStrength) = prefs.edit().putString(KEY_HAPTIC_STRENGTH, value.name).apply()
    fun setTelexEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_TELEX, value).apply()
    fun setPopupEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_POPUP, value).apply()
    fun setSuggestionsEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_SUGGESTIONS, value).apply()
    fun setLearnWords(value: Boolean) = prefs.edit().putBoolean(KEY_LEARN, value).apply()
    fun setSuggestionCount(value: Int) = prefs.edit().putInt(KEY_SUGGEST_COUNT, value.coerceIn(3, 5)).apply()
    fun setClipboardEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_CLIPBOARD, value).apply()
    fun setClipboardDays(value: Int) = prefs.edit().putInt(KEY_CLIPBOARD_DAYS, value).apply()
    fun setAutoCap(value: Boolean) = prefs.edit().putBoolean(KEY_AUTO_CAP, value).apply()
    fun setDoubleSpacePeriod(value: Boolean) = prefs.edit().putBoolean(KEY_DOUBLE_SPACE, value).apply()
    fun setComposingUnderline(value: Boolean) = prefs.edit().putBoolean(KEY_UNDERLINE, value).apply()

    private fun read() = KeyboardPrefs(
        mode = parseEnum(prefs.getString(KEY_MODE, null), AppearanceMode.SYSTEM),
        colorSource = parseEnum(prefs.getString(KEY_COLOR_SOURCE, null), ColorSource.SYSTEM),
        customSeed = prefs.getInt(KEY_SEED, ZeroPalette.DefaultSeed),
        soundEnabled = prefs.getBoolean(KEY_SOUND, true),
        soundVolume = prefs.getFloat(KEY_VOLUME, 0.7f).coerceIn(0f, 1f),
        soundPack = prefs.getString(KEY_SOUND_PACK, "cream_linear") ?: "cream_linear",
        hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
        hapticStrength = parseEnum(prefs.getString(KEY_HAPTIC_STRENGTH, null), HapticStrength.MEDIUM),
        telexEnabled = prefs.getBoolean(KEY_TELEX, true),
        popupEnabled = prefs.getBoolean(KEY_POPUP, true),
        suggestionsEnabled = prefs.getBoolean(KEY_SUGGESTIONS, true),
        learnWords = prefs.getBoolean(KEY_LEARN, true),
        suggestionCount = prefs.getInt(KEY_SUGGEST_COUNT, 4).coerceIn(3, 5),
        clipboardEnabled = prefs.getBoolean(KEY_CLIPBOARD, true),
        clipboardDays = prefs.getInt(KEY_CLIPBOARD_DAYS, 30),
        autoCap = prefs.getBoolean(KEY_AUTO_CAP, true),
        doubleSpacePeriod = prefs.getBoolean(KEY_DOUBLE_SPACE, true),
        composingUnderline = prefs.getBoolean(KEY_UNDERLINE, false),
    )

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, default: T): T =
        runCatching { enumValueOf<T>(raw ?: default.name) }.getOrDefault(default)

    private companion object {
        const val NAME = "neo_keyboard"
        const val KEY_MODE = "mode"
        const val KEY_COLOR_SOURCE = "color_source"
        const val KEY_SEED = "custom_seed"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_VOLUME = "sound_volume"
        const val KEY_SOUND_PACK = "sound_pack"
        const val KEY_HAPTICS = "haptics_enabled"
        const val KEY_HAPTIC_STRENGTH = "haptic_strength"
        const val KEY_TELEX = "telex_enabled"
        const val KEY_POPUP = "popup_enabled"
        const val KEY_SUGGESTIONS = "suggestions_enabled"
        const val KEY_LEARN = "learn_words"
        const val KEY_SUGGEST_COUNT = "suggestion_count"
        const val KEY_CLIPBOARD = "clipboard_enabled"
        const val KEY_CLIPBOARD_DAYS = "clipboard_days"
        const val KEY_AUTO_CAP = "auto_cap"
        const val KEY_DOUBLE_SPACE = "double_space_period"
        const val KEY_UNDERLINE = "composing_underline"
    }
}
