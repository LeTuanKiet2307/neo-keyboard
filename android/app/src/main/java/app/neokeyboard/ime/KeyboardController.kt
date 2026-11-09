package app.neokeyboard.ime

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.inputmethod.EditorInfo
import app.neokeyboard.data.KeyboardPrefs
import app.neokeyboard.data.KeyboardSettings
import app.neokeyboard.haptics.KeyHaptics
import app.neokeyboard.sound.KeySoundPlayer
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
    private val sound: KeySoundPlayer,
    private val haptics: KeyHaptics,
    private val suggestions: SuggestionEngine = NoOpSuggestions,
    private val commitProcessor: CommitProcessor = CommitProcessor.Identity,
    private val clipboardRepo: app.neokeyboard.clipboard.ClipboardRepository? = null,
    private val triggers: app.neokeyboard.trigger.TriggerStore? = null,
) {
    private val telex = TelexEngine()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var repeatTick = 0
    private val repeater = BackspaceRepeater(
        onDelete = { backspaceOnce() },
        onTick = {
            // Nhịp lặp nhanh hơn độ dài âm: phát âm cách một nhịp để không thành tiếng rè liên tục.
            val p = prefs.state.value
            if (repeatTick++ % 2 == 0) sound.playPress(p.soundEnabled, p.soundVolume * 0.6f)
            feedback.post { haptics.press(p.hapticsEnabled, p.hapticStrength) }
        },
    )

    /**
     * Rung là binder call đồng bộ sang vibrator service; đẩy khỏi main thread để thao tác chữ
     * (cũng cần main thread) không phải xếp hàng sau nó.
     */
    private val feedbackThread = android.os.HandlerThread("neo-feedback").apply { start() }
    private val feedback = android.os.Handler(feedbackThread.looper)
    private var telexActive = true

    private val ic get() = ime.currentInputConnection
    private fun editor(): TextEditor = TextEditor(ic)

    // ---- Gõ thẳng (mặc định): không composing → app không vẽ gạch chân dưới từ đang gõ ----
    private val tracker = CursorTracker()
    /** Chữ của từ đang gõ hiện trong ô nhập (chế độ gõ thẳng). */
    private var shown = ""
    /** Vài ký tự cuối chắc chắn trước con trỏ (xem [TypingContext]); null = không biết. */
    private var recent: String? = null
    /** Shift ONCE do tự viết hoa bật (không phải người dùng bấm). */
    private var autoShifted = false

    private fun inline(): Boolean = !prefs.state.value.composingUnderline

    /** Ghi văn bản của chính bàn phím (dấu cách, dấu câu, dán...) và cập nhật dự đoán con trỏ. */
    private fun ownCommit(text: String, remember: Boolean = true) {
        if (text.isEmpty()) return
        ic?.commitText(text, 1)
        tracker.ownEdit(text.length)
        recent = if (remember && !ui.passwordField) TypingContext.append(recent, text) else null
    }

    /**
     * Hiện [new] cho từ đang gõ. Gõ thẳng: giữ tiền tố chung, xóa phần đuôi khác rồi ghi phần mới.
     * Lưới an toàn: trước khi XÓA, đọc lại chữ trước con trỏ; không khớp từ đang gõ (người dùng đã
     * dời con trỏ mà update chưa tới) thì trả false, không xóa gì — thà mất dấu Telex còn hơn xóa
     * nhầm chữ của người dùng. Chỉ đọc khi cần xóa, gõ nối chữ không tốn IPC đọc.
     */
    private fun showComposing(new: String): Boolean {
        val conn = ic ?: run { shown = new; return true }
        if (!inline()) {
            conn.setComposingText(new, 1)
            shown = new
            return true
        }
        val (del, ins) = TypingContext.diff(shown, new)
        if (del == 0 && ins.isEmpty()) return true
        if (del > 0) {
            val before = runCatching { conn.getTextBeforeCursor(shown.length, 0)?.toString() }.getOrNull()
            if (before != shown) return false
        }
        conn.beginBatchEdit()
        if (del > 0) conn.deleteSurroundingText(del, 0)
        if (ins.isNotEmpty()) conn.commitText(ins, 1)
        conn.endBatchEdit()
        tracker.ownEdit(ins.length - del)
        shown = new
        return true
    }

    /** Kết thúc vùng của từ đang gõ (chữ giữ nguyên trong ô). */
    private fun endComposingRegion() {
        if (!inline()) ic?.finishComposingText()
        shown = ""
    }

    private fun maybeAutoCap() {
        val p = prefs.state.value
        if (!p.autoCap || !telexActive || ui.passwordField) return
        if (ui.shiftMode == ShiftMode.OFF && TypingContext.shouldCap(recent)) {
            ui.shiftMode = ShiftMode.ONCE
            autoShifted = true
        }
    }

    private fun recheckAutoCap() {
        if (autoShifted && !TypingContext.shouldCap(recent)) {
            ui.shiftMode = ShiftMode.OFF
            autoShifted = false
        } else maybeAutoCap()
    }

    private fun suggestFor(composed: String): List<String> {
        if (!prefs.state.value.suggestionsEnabled) return emptyList()
        val list = suggestions.onComposingChanged(composed)
        // Từ đang gõ viết hoa chữ đầu (đầu câu/shift) → gợi ý cũng viết hoa theo.
        return if (composed.firstOrNull()?.isUpperCase() == true) list.map { w -> w.replaceFirstChar { it.uppercaseChar() } } else list
    }

    private fun suggestNext(): List<String> =
        if (prefs.state.value.suggestionsEnabled) suggestions.onSpace() else emptyList()

    /** NeoKey down: phản hồi tức thì; backspace xóa một đơn vị ngay. */
    fun keyDown(key: Key) {
        val p = prefs.state.value
        if (key.special == Special.BACKSPACE) backspaceOnce()
        sound.playPress(p.soundEnabled, p.soundVolume)
        feedback.post { haptics.press(p.hapticsEnabled, p.hapticStrength) }
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
        repeatTick = 0
        repeater.start(scope)
    }

    /** Vuốt trái trên backspace: hộp repeater, xóa từ một lần. */
    fun backspaceWordDelete() {
        repeater.stop()
        if (!telex.isEmpty && showComposing("")) {
            endComposingRegion()
            resetWord()
        } else {
            endComposingRegion()
            resetWord()
            editor().deleteWord()
            tracker.ownEdit(null)
            recent = null
        }
        recheckAutoCap()
        val p = prefs.state.value
        sound.playPress(p.soundEnabled, p.soundVolume)
        feedback.post { haptics.press(p.hapticsEnabled, p.hapticStrength) }
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
        if (!telex.isEmpty) {
            if (showComposing(word)) {
                endComposingRegion()
                recent = TypingContext.append(recent, word)
            } else {
                endComposingRegion()
                ownCommit(word)
            }
            suggestions.onWordCommitted(word)
            resetWord()
            ownCommit(" ")
        } else {
            ownCommit("$word ")
        }
        resetShiftOnce()
        maybeAutoCap()
        ui.candidates = suggestNext()
    }

    fun openClipboard() {
        if (ui.passwordField || !prefs.state.value.clipboardEnabled) return
        commitComposing()
        ui.clipboardOpen = true
    }

    fun closeClipboard() {
        ui.clipboardOpen = false
    }

    fun pasteClip(text: String) {
        if (text.isEmpty()) return
        commitComposing()
        ownCommit(text)
        ui.clipboardOpen = false
        val p = prefs.state.value
        sound.playPress(p.soundEnabled, p.soundVolume)
        haptics.press(p.hapticsEnabled, p.hapticStrength)
    }

    /** Giữ phím ⚡: mở dải chọn các mục nhập nhanh (chỉ nhãn). */
    fun openTriggerMenu() {
        val st = triggers?.state?.value ?: app.neokeyboard.trigger.TriggerStore.State()
        val activeId = st.active?.id
        ui.triggerItems = st.entries.map {
            TriggerChip(it.id, it.label, it.kind == app.neokeyboard.trigger.TriggerKind.SECRET, it.id == activeId)
        }
        ui.clipboardOpen = false
        ui.triggerMenuOpen = true
    }

    fun closeTriggerMenu() {
        ui.triggerMenuOpen = false
        ui.triggerItems = emptyList()
    }

    /** Chọn một mục từ dải: nhập ngay và gán làm mục mặc định của phím ⚡. */
    fun pickTrigger(id: String) {
        val store = triggers ?: return
        val entry = store.state.value.entries.firstOrNull { it.id == id } ?: return
        store.setActive(id)
        closeTriggerMenu()
        insertTrigger(entry)
    }

    /**
     * Nhập nội dung trigger. Đi thẳng commitText: KHÔNG qua gợi ý/học từ, KHÔNG vào clipboard,
     * nên mật khẩu không bị lưu ở đâu khác ngoài ô đang nhập.
     */
    private fun insertTrigger(entry: app.neokeyboard.trigger.TriggerEntry) {
        val text = triggers?.resolve(entry) ?: return
        commitComposing()
        // Bí mật không được lưu lại cả trong bộ nhớ "recent".
        ownCommit(text, remember = entry.kind != app.neokeyboard.trigger.TriggerKind.SECRET)
        resetShiftOnce()
        if (entry.pressEnter) enter()
    }

    /** Nút ⚙ trên dải: mở màn cài đặt và cất bàn phím. */
    fun openSettings(section: String? = null) {
        commitComposing()
        closeTriggerMenu()
        val intent = android.content.Intent(ime, app.neokeyboard.ui.settings.SettingsActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (section != null) intent.putExtra(app.neokeyboard.ui.settings.SettingsActivity.EXTRA_SECTION, section)
        runCatching { ime.startActivity(intent) }
        ime.requestHideSelf(0)
    }

    fun copySelection(): Boolean {
        if (ui.passwordField) return false
        val selected = try { ic?.getSelectedText(0)?.toString() } catch (_: Exception) { null }
        if (selected.isNullOrEmpty()) return false
        val cm = ime.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        cm?.setPrimaryClip(android.content.ClipData.newPlainText("Neo Keyboard", selected))
        clipboardRepo?.add(selected)
        return true
    }

    fun pasteFromSystem(): Boolean {
        if (ui.passwordField) return false
        val cm = ime.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val text = cm?.primaryClip?.getItemAt(0)?.text?.toString()
        if (text.isNullOrBlank()) return false
        clipboardRepo?.clipboardFromSystemText(text)
        pasteClip(text)
        return true
    }

    private fun handleKey(key: Key, p: KeyboardPrefs) {
        when (key.special) {
            Special.SHIFT -> { ui.shiftMode = nextShift(ui.shiftMode); autoShifted = false; return }
            Special.BACKSPACE -> { repeater.stop(); return }
            Special.ENTER -> { enter(); return }
            Special.SPACE -> {
                commitComposing()
                if (p.doubleSpacePeriod && telexActive && TypingContext.canDoubleSpacePeriod(recent)) {
                    // "chữ␣" + space → "chữ.␣": xóa đúng dấu cách bàn phím vừa ghi (recent biết chắc).
                    ic?.let { c ->
                        c.beginBatchEdit(); c.deleteSurroundingText(1, 0); c.commitText(". ", 1); c.endBatchEdit()
                    }
                    tracker.ownEdit(1)
                    recent = TypingContext.append(TypingContext.dropLast(recent), ". ")
                } else {
                    ownCommit(" ")
                }
                resetShiftOnce()
                maybeAutoCap()
                ui.candidates = suggestNext()
                return
            }
            Special.TO_SYMBOLS -> { commitComposing(); ui.page = KeyboardPage.SYMBOLS; return }
            Special.TO_LETTERS -> { commitComposing(); ui.page = KeyboardPage.LETTERS; return }
            Special.TRIGGER -> {
                val active = triggers?.state?.value?.active
                if (active == null) openTriggerMenu() else insertTrigger(active)
                return
            }
            Special.NONE -> {}
        }
        if (key.isSpacer) return
        val ch = key.text.firstOrNull() ?: return
        if (telexActive && p.telexEnabled && ch.isLetter()) {
            val c = if (ui.shifted) ch.uppercaseChar() else ch.lowercaseChar()
            var composed = telex.append(c)
            if (!showComposing(composed)) {
                // Con trỏ đã rời từ cũ: bỏ từ cũ (chữ vẫn nằm trong ô), bắt đầu từ mới tại chỗ mới.
                shown = ""
                resetWord()
                recent = null
                composed = telex.append(c)
                showComposing(composed)
            }
            ui.composing = composed
            ui.candidates = suggestFor(composed)
            resetShiftOnce()
        } else {
            commitComposing()
            val out = if (ui.shifted) ch.uppercaseChar() else ch
            ownCommit(out.toString())
            resetShiftOnce()
            maybeAutoCap()
        }
    }

    /**
     * Một đơn vị xóa: composing (Telex) > selection/grapheme (SPEC §8.2).
     *
     * Ngoài vùng composing, gửi KEYCODE_DEL thay vì getSelectedText + getTextBeforeCursor:
     * hai lệnh đọc đó là IPC ĐỒNG BỘ, phải chờ UI thread của app đang gõ rảnh (app Compose nặng
     * như chat System Zero có thể bắt chờ hàng chục ms mỗi lần xóa → cảm giác "delay sâu").
     * Key event là một chiều, editor tự xóa selection hoặc đúng một grapheme (emoji/ZWJ/flag).
     */
    private fun backspaceOnce() {
        if (!telex.isEmpty) {
            val composed = telex.backspace()
            if (!showComposing(composed)) {
                // Không chắc chữ trước con trỏ còn là từ đang gõ: để editor tự xóa một ký tự.
                endComposingRegion()
                resetWord()
                recent = null
                ime.sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DEL)
                tracker.ownEdit(null)
            } else if (composed.isEmpty()) {
                endComposingRegion()
                resetWord()
            } else {
                ui.composing = composed
                ui.candidates = suggestFor(composed)
            }
        } else {
            ime.sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DEL)
            tracker.ownEdit(null)
            recent = TypingContext.dropLast(recent)
        }
        recheckAutoCap()
    }

    private fun enter() {
        commitComposing()
        val info = ime.currentInputEditorInfo
        val options = info?.imeOptions ?: 0
        val action = options and EditorInfo.IME_MASK_ACTION
        val noEnterAction = (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic?.performEditorAction(action)
            recent = null
        } else {
            ownCommit("\n")
            resetShiftOnce()
            maybeAutoCap()
        }
    }

    /** Commit từ đang composing (qua autocorrect seam) rồi báo hook học. */
    private fun commitComposing() {
        val composed = telex.composing
        if (composed.isNotEmpty()) {
            val final = if (ui.passwordField) composed else commitProcessor.process(composed)
            if (final != composed) showComposing(final)
            if (!inline() && final == composed) ic?.finishComposingText()
            if (!ui.passwordField) suggestions.onWordCommitted(final)
            recent = TypingContext.append(recent, final)
        }
        endComposingRegion()
        resetWord()
    }

    private fun resetWord() {
        telex.reset()
        ui.composing = ""
        ui.candidates = emptyList()
        suggestions.reset()
    }

    /** Con trỏ nhảy khỏi từ đang gõ (người dùng chạm chỗ khác) → giữ chữ, ngừng bám. */
    fun onSelectionChanged(newSelStart: Int, newSelEnd: Int, candidatesEnd: Int) {
        val jumped = tracker.onUpdate(newSelStart, newSelEnd)
        if (jumped) {
            recent = null
            if (autoShifted) { ui.shiftMode = ShiftMode.OFF; autoShifted = false }
        }
        if (telex.isEmpty) return
        val moved = if (inline()) jumped
        else newSelStart != newSelEnd || (candidatesEnd >= 0 && newSelStart != candidatesEnd)
        if (moved) {
            if (!inline()) ic?.finishComposingText()
            shown = ""
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

    fun onStartInput(info: EditorInfo? = null) {
        resetWord()
        shown = ""
        closeTriggerMenu()
        ui.shiftMode = ShiftMode.OFF
        autoShifted = false
        tracker.reset(info?.initialSelEnd ?: -1)
        recent = if (info != null && info.initialSelStart == 0 && info.initialSelEnd == 0) "" else null
        if (recent == null && info != null && info.initialCapsMode != 0) recent = ""
        maybeAutoCap()
    }

    fun onFinishInput() {
        repeater.stop()
        commitComposing()
    }

    fun close() {
        repeater.stop()
        scope.cancel()
        feedbackThread.quitSafely()
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
        autoShifted = false
    }
}
