package app.neokeyboard.ime

import android.view.inputmethod.InputConnection
import java.text.BreakIterator

/**
 * Bọc InputConnection theo SPEC §8: xóa grapheme cluster (không tách emoji/ZWJ/flag),
 * xóa từ, xóa selection. Không chuẩn hóa trước khi đo (không đổi văn bản host).
 */
class TextEditor(private val ic: InputConnection?) {

    /** Xóa selection nếu có. Trả true nếu đã xóa. */
    fun deleteSelection(): Boolean {
        val selected = try { ic?.getSelectedText(0) } catch (_: Exception) { null } ?: return false
        if (selected.isEmpty()) return false
        return try { ic?.commitText("", 1) == true } catch (_: Exception) { false }
    }

    fun hasSelection(): Boolean =
        try { ic?.getSelectedText(0)?.isNotEmpty() == true } catch (_: Exception) { false }

    /** Xóa một grapheme cluster trước con trỏ (SPEC §8.5). */
    fun deleteGrapheme(): Boolean {
        val conn = ic ?: return false
        val before = try { conn.getTextBeforeCursor(64, 0)?.toString() } catch (_: Exception) { null }
        if (before.isNullOrEmpty()) return deleteFallback()
        val length = graphemeTailLength(before)
        if (length <= 0) return deleteFallback()
        return if (try { conn.deleteSurroundingText(length, 0) } catch (_: Exception) { false }) {
            true
        } else deleteFallback()
    }

    /** Xóa một từ trước con trỏ (SPEC §8.6). */
    fun deleteWord(): Boolean {
        val conn = ic ?: return false
        if (hasSelection()) return try { conn.commitText("", 1) } catch (_: Exception) { false }
        val before = try { conn.getTextBeforeCursor(64, 0)?.toString() } catch (_: Exception) { null }
        if (before.isNullOrEmpty()) return deleteGrapheme()
        val span = wordSpanBeforeCursor(before)
        if (span <= 0) return deleteGrapheme()
        return if (try { conn.deleteSurroundingText(span, 0) } catch (_: Exception) { false }) {
            true
        } else deleteFallback()
    }

    private fun deleteFallback(): Boolean {
        val conn = ic ?: return false
        if (try { conn.deleteSurroundingTextInCodePoints(1, 0) } catch (_: Exception) { false }) return true
        // InputConnection không có sendDownUpKeyEvents: gửi cặp down/up qua sendKeyEvent.
        return try {
            conn.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DEL)) &&
                conn.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DEL))
        } catch (_: Exception) { false }
    }

    companion object {
        /** Độ dài grapheme cluster cuối của [text] (UTF-16 length), 0 nếu rỗng. */
        fun graphemeTailLength(text: String): Int {
            if (text.isEmpty()) return 0
            val iterator = BreakIterator.getCharacterInstance()
            iterator.setText(text)
            var last = iterator.first()
            var next = iterator.next()
            var prev = last
            while (next != BreakIterator.DONE) {
                prev = last
                last = next
                next = iterator.next()
            }
            // prev là biên trước cluster cuối.
            val length = text.length - prev
            return length.coerceIn(1, text.length)
        }

        /** Độ dài span từ cần xóa để xóa một từ trước con trỏ (bỏ space cuối rồi quét ngược). */
        fun wordSpanBeforeCursor(before: String): Int {
            if (before.isEmpty()) return 0
            var end = before.length
            while (end > 0 && before[end - 1].isWhitespace()) end--
            if (end == 0) return before.length
            var start = end
            while (start > 0) {
                val c = before[start - 1]
                if (c.isWhitespace() || c in ".,;:!?()[]{}\u201C\u201D\u2018\u2019\"\n") break
                start--
            }
            if (start == end) {
                // Ký tự cuối là dấu câu: chỉ xóa lại dấu đó.
                return before.length - (end - 1)
            }
            // Span gồm từ + space cuối đã bỏ.
            return before.length - start
        }
    }
}
