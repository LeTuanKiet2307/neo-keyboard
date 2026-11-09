package app.neokeyboard.ime

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.inputmethod.EditorInfo
import app.neokeyboard.data.KeyboardPrefs
import app.neokeyboard.data.KeyboardSettings
import app.neokeyboard.haptics.KeyHaptics
import app.neokeyboard.sound.CreamSoundPlayer
import app.neokeyboard.suggest.CommitProcessor
import app.neokeyboard.suggest.NoOpSuggestions
import app.neokeyboard.suggest.SuggestionEngine
import app.neokeyboard.telex.TelexEngine

/**
 * Sự kiện phím → Telex → InputConnection. Giữ engine Telex thuần và pipeline composing;
 * âm/rung gọi ở đây để UI chỉ việc báo nhấn/nhả. Cờ editor (password/email/số) tắt Telex.
 */
class KeyboardController(
    private val ime: InputMethodService,
    private val ui: KeyboardUiState,
    private val prefs: KeyboardSettings,
    private val sound: CreamSoundPlayer,
    private val haptics: KeyHaptics,
    private val suggestions: SuggestionEngine = NoOpSuggestions,
    private val commitProcessor: CommitProcessor = CommitProcessor.Identity,
) {
    private val telex = TelexEngine()
    private var telexActive = true

    private val ic get() = ime.currentInputConnection

    /** UI gọi khi chạm xuống: phản hồi tức thì rồi thực hiện phím. */
    fun keyPressed(key: Key) {
        val p = prefs.state.value
        sound.playPress(p.soundEnabled, p.soundVolume)
        haptics.press(p.hapticsEnabled, p.hapticStrength)
        handleKey(key, p)
    }

    /** UI gọi khi nhả: chỉ âm release. */
    fun keyReleased() {
        val p = prefs.state.value
        sound.playRelease(p.soundEnabled, p.soundVolume)
    }

    /** Chạm một gợi ý (GĐ2): thay từ đang composing bằng gợi ý + khoảng trắng. */
    fun pickCandidate(word: String) {
        ic?.commitText("$word ", 1)
        resetWord()
    }

    private fun handleKey(key: Key, p: KeyboardPrefs) {
        when (key.special) {
            Special.SHIFT -> { ui.shiftMode = nextShift(ui.shiftMode); return }
            Special.BACKSPACE -> { backspace(); return }
            Special.ENTER -> { enter(); return }
            Special.SPACE -> {
                commitComposing()
                ic?.commitText(" ", 1)
                resetShiftOnce()
                return
            }
            Special.TO_SYMBOLS -> { commitComposing(); ui.page = KeyboardPage.SYMBOLS; return }
            Special.TO_LETTERS -> { commitComposing(); ui.page = KeyboardPage.LETTERS; return }
            Special.NONE -> {}
        }
        if (key.isSpacer) return
        val ch = key.text.firstOrNull() ?: return
        if (telexActive && p.telexEnabled && ch.isLetter()) {
            val c = if (ui.shifted) ch.uppercaseChar() else ch.lowercaseChar()
            val composed = telex.append(c)
            ic?.setComposingText(composed, 1)
            ui.composing = composed
            ui.candidates = suggestions.onComposingChanged(composed)
            resetShiftOnce()
        } else {
            commitComposing()
            val out = if (ui.shifted) ch.uppercaseChar() else ch
            ic?.commitText(out.toString(), 1)
            resetShiftOnce()
        }
    }

    private fun backspace() {
        if (!telex.isEmpty) {
            val composed = telex.backspace()
            if (composed.isEmpty()) {
                ic?.commitText("", 1)
                ui.composing = ""
            } else {
                ic?.setComposingText(composed, 1)
                ui.composing = composed
            }
            ui.candidates = suggestions.onComposingChanged(composed)
        } else {
            ic?.deleteSurroundingText(1, 0)
        }
    }

    private fun enter() {
        commitComposing()
        val info = ime.currentInputEditorInfo
        val options = info?.imeOptions ?: 0
        val action = options and EditorInfo.IME_MASK_ACTION
        val noEnterAction = (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic?.performEditorAction(action)
        } else {
            ic?.commitText("\n", 1)
        }
    }

    /** Commit từ đang composing (qua autocorrect seam) rồi báo hook học. */
    private fun commitComposing() {
        val composed = telex.composing
        if (composed.isNotEmpty()) {
            val final = commitProcessor.process(composed)
            ic?.commitText(final, 1)
            suggestions.onWordCommitted(final)
        }
        resetWord()
    }

    private fun resetWord() {
        telex.reset()
        ui.composing = ""
        ui.candidates = emptyList()
        suggestions.reset()
    }

    /** Con trỏ nhảy khỏi vùng composing (người dùng chạm chỗ khác) → giữ chữ, ngừng bám. */
    fun onSelectionChanged(newSelStart: Int, newSelEnd: Int, candidatesEnd: Int) {
        if (telex.isEmpty) return
        if (newSelStart != newSelEnd || (candidatesEnd >= 0 && newSelStart != candidatesEnd)) {
            ic?.finishComposingText()
            resetWord()
        }
    }

    /** Đọc cờ editor để bật/tắt Telex và chọn trang phù hợp. */
    fun onEditorChanged(info: EditorInfo?) {
        val type = info?.inputType ?: InputType.TYPE_CLASS_TEXT
        val clazz = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION
        val password = isPassword(clazz, variation)
        val number = clazz == InputType.TYPE_CLASS_NUMBER ||
            clazz == InputType.TYPE_CLASS_PHONE ||
            clazz == InputType.TYPE_CLASS_DATETIME
        val emailUri = variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_URI
        telexActive = !password && !number && !emailUri
        ui.passwordField = password
        ui.page = if (number) KeyboardPage.SYMBOLS else KeyboardPage.LETTERS
    }

    fun onStartInput() {
        resetWord()
        ui.shiftMode = ShiftMode.OFF
    }

    fun onFinishInput() {
        commitComposing()
    }

    private fun isPassword(clazz: Int, variation: Int): Boolean =
        variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            (clazz == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)

    private fun nextShift(mode: ShiftMode): ShiftMode = when (mode) {
        ShiftMode.OFF -> ShiftMode.ONCE
        ShiftMode.ONCE -> ShiftMode.LOCK
        ShiftMode.LOCK -> ShiftMode.OFF
    }

    private fun resetShiftOnce() {
        if (ui.shiftMode == ShiftMode.ONCE) ui.shiftMode = ShiftMode.OFF
    }
}