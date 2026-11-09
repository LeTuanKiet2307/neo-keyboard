package app.neokeyboard.suggest

/**
 * Engine gợi ý tiếng Việt (SPEC §5.3): completion theo composing + next-word sau space.
 * MVP dùng [NoOpSuggestions] (luôn rỗng); M2 thay bằng LexiconSuggestionEngine.
 */
interface SuggestionEngine {
    /** Chuỗi đang composing đổi → trả danh sách gợi ý (MVP: rỗng). */
    fun onComposingChanged(composing: String): List<String>

    /** Chuỗi đang composing đổi (chuẩn SPEC §5.3). */
    fun onComposingChanged(composing: String, limit: Int): List<String> =
        onComposingChanged(composing).take(limit)

    /** Vừa chèn khoảng trắng → gợi ý từ tiếp theo (MVP: rỗng). */
    fun onSpace(limit: Int = 4): List<String> = emptyList()

    /** Một từ vừa commit → hook để học/ghi nhớ (MVP: không làm gì). */
    fun onWordCommitted(word: String)

    /** Sang từ mới / reset con trỏ. */
    fun reset()
}

object NoOpSuggestions : SuggestionEngine {
    override fun onComposingChanged(composing: String): List<String> = emptyList()
    override fun onWordCommitted(word: String) {}
    override fun reset() {}
}
