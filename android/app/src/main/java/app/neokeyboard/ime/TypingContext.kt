package app.neokeyboard.ime

/**
 * Logic thuần cho chế độ gõ thẳng (không composing → không gạch chân):
 * - [diff]: biến chữ đang hiện thành chữ mới bằng ít thao tác nhất (giữ tiền tố chung).
 * - "recent": vài ký tự cuối bàn phím BIẾT CHẮC nằm trước con trỏ, để tự viết hoa / hai lần
 *   space mà không phải hỏi app (IPC đồng bộ). null = không biết; [UNKNOWN] ở đầu = phần trước
 *   đó không biết; "" = đầu ô nhập.
 */
object TypingContext {
    const val UNKNOWN = '￿'
    private const val KEEP = 8
    private val sentenceEnd = Regex("[.!?…]\\s+$")

    /** Số ký tự cần xóa ở cuối [old] và chuỗi cần ghi thêm để thành [new]. */
    fun diff(old: String, new: String): Pair<Int, String> {
        var p = 0
        val max = minOf(old.length, new.length)
        while (p < max && old[p] == new[p]) p++
        if (p > 0 && p < old.length && Character.isHighSurrogate(old[p - 1])) p--
        return (old.length - p) to new.substring(p)
    }

    fun append(recent: String?, text: String): String {
        if (text.isEmpty()) return recent ?: UNKNOWN.toString()
        val joined = (recent ?: UNKNOWN.toString()) + text
        return if (joined.length <= KEEP) joined else UNKNOWN + joined.takeLast(KEEP - 1)
    }

    /** Sau một lần xóa một ký tự. */
    fun dropLast(recent: String?): String? {
        if (recent == null || recent.isEmpty()) return recent
        val last = recent.last()
        if (last == UNKNOWN || Character.isLowSurrogate(last)) return null
        val out = recent.dropLast(1)
        return if (out == UNKNOWN.toString()) null else out
    }

    fun shouldCap(recent: String?): Boolean {
        if (recent == null) return false
        if (recent.isEmpty()) return true
        if (recent.last() == '\n') return true
        return sentenceEnd.containsMatchIn(recent)
    }

    /** "chữ␣" + space → ".␣" (kiểu Gboard). */
    fun canDoubleSpacePeriod(recent: String?): Boolean {
        if (recent == null || recent.length < 2) return false
        return recent.last() == ' ' && recent[recent.length - 2].isLetterOrDigit()
    }
}

/**
 * Theo dõi vị trí con trỏ mà bàn phím dự đoán sau các lần ghi của chính nó, để nhận ra người
 * dùng chạm chỗ khác. Mỗi lần ghi có hiệu lực sinh một onUpdateSelection; các update đến trễ
 * (app bận) được trừ dần qua [pending] thay vì bị hiểu nhầm là con trỏ nhảy.
 */
class CursorTracker {
    var expected: Int = -1
        private set
    var pending: Int = 0
        private set

    fun reset(initialSelEnd: Int) {
        expected = initialSelEnd
        pending = 0
    }

    /** Bàn phím vừa ghi: con trỏ dịch [delta] ký tự, null = không đoán được (Enter/Del...). */
    fun ownEdit(delta: Int?) {
        expected = if (delta == null || expected < 0) -1 else expected + delta
        pending++
    }

    /** Trả true nếu con trỏ đã bị người dùng/ứng dụng dời khỏi chỗ dự đoán. */
    fun onUpdate(selStart: Int, selEnd: Int): Boolean {
        if (selStart != selEnd) {
            pending = 0
            expected = selEnd
            return true
        }
        if (pending > 0) {
            pending--
            if (pending > 0) return false
        }
        val jumped = expected >= 0 && selEnd != expected
        expected = selEnd
        return jumped
    }
}
