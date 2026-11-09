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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Sự kiện phím (down/up theo SPEC §3.2): down phát âm/rung + backspace xóa ngay;
 * up trong biên mới thực hiện phím. Giữ engine Telex thuần và pipeline composing;
 * âm/rung gọi ở đây để UI chỉ việc báo down/up/cancel.
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repeater = BackspaceRepeater(
        onDelete = { backspaceOnce() },
        onTick = {
            val p = prefs.state.value
            sound.playPress(p.soundEnabled, p.soundVolume * 0.7f)
            haptics.press(p.hapticsEnabled, p.hapticStrength)
        },
    )
    private var telexActive = true

    private val ic get() = ime.currentInputConnection
    private fun editor(): TextEditor = TextEditor(ic)

    /** NeoKey down: phản hồi tức thì; backspace xóa một đơn vị ngay. */
    fun keyDown(key: Key) {
        val p = prefs.state.value
        sound.playPress(p.soundEnabled, p.soundVolume)
        haptics.press(p.hapticsEnabled, p.hapticStrength)
        if (key.special == Special.BACKSPACE) backspaceOnce()
    }

    /** NeoKey up: phím thường chỉ thực hiện khi trong biên và chưa cancel. */
    fun keyUp(key: Key, inside: Boolean) {
        val p = prefs.state.value
        sound.playRelease(p.soundEnabled, p.soundVolume)
        if (key.special == Special.BACKSPACE) {
            repeater.stop()
            return
        }
        if (!inside) return
        handleKey(key, p)
    }

    fun keyCancel(key: Key) {
        val p = prefs.state.value
        sound.playRelease(p.soundEnabled, p.soundVolume)
        repeater.stop()
    }

    /** NeoKey báo long-press: bắt đầu lặp xóa (đã xóa 1 lần lúc down). */
    fun backspaceRepeatStart() {
        repeater.start(scope)
    }

    /** Vuốt trái trên backspace: hộp repeater, xóa từ một lần. */
    fun backspaceWordDelete() {
        repeater.stop()
        if (!telex.isEmpty) {
            ic?.commitText("", 1)
            resetWord()
        } else editor().deleteWord()
        val p = prefs.state.value
        sound.playPress(p.soundEnabled, p.soundVolume)
        haptics.press(p.hapticsEnabled, p.hapticStrength)
    }

    /** Tương thích test cũ: coi như down+up trong biên. */
    fun keyPressed(key: Key) {
        keyDown(key)
        keyUp(key, inside = true)
    }

    fun keyReleased() {
        val p = prefs.state.value
        repeater.stop()
        sound.playRelease(p.soundEnabled, p.soundVolume)
    }

    /** Chạm một gợi ý (GĐ2): thay từ đang composing bằng gợi ý + khoảng trắng. */
    fun pickCandidate(word: String) {
        ic?.commitText("$word ", 1)
        resetWord()
        ui.candidates = suggestions.onSpace()
    }

    private fun handleKey(key: Key, p: KeyboardPrefs) {
        when (key.special) {
            Special.SHIFT -> { ui.shiftMode = nextShift(ui.shiftMode); return }
            Special.BACKSPACE -> { repeater.stop(); return }
            Special.ENTER -> { enter(); return }
            Special.SPACE -> {
                commitComposing()
                ic?.commitText(" ", 1)
                resetShiftOnce()
                ui.candidates = suggestions.onSpace()
                return
            }
            Special.TO_SYMBOLS -> { commitComposing(); ui.page = KeyboardPage.SYMBOLS; return }
            Special.TO_LETTERS -> { commitComposing(); ui.page = KeyboardPage.LETTERS; return }
            Special.EMOJI -> {
                commitComposing()
                ic?.commitText("😊", 1)
                resetShiftOnce()
                return
            }
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

    /** Một đơn vị xóa: selection > composing (Telex) > grapheme (SPEC §8.2). */
    private fun backspaceOnce() {
        if (editor().hasSelection()) {
            editor().deleteSelection()
            resetWord()
            return
        }
        if (!telex.isEmpty) {
            val composed = telex.backspace()
            if (composed.isEmpty()) {
                ic?.setComposingText("", 1)
                ic?.finishComposingText()
                resetWord()
            } else {
                ic?.setComposingText(composed, 1)
                ui.composing = composed
                ui.candidates = suggestions.onComposingChanged(composed)
            }
        } else {
            editor().deleteGrapheme()
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
        if (composed.isNotEmpty() && !ui.passwordField) {
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
        repeater.stop()
        commitComposing()
    }

    fun close() {
        repeater.stop()
        scope.cancel()
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
