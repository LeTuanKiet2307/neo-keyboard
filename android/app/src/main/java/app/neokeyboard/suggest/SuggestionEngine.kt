package app.neokeyboard.suggest

/**
 * Mối nối Giai đoạn 2: engine gợi ý/đoán từ tiếng Việt có học. MVP dùng [NoOpSuggestions]
 * (luôn rỗng) nên dải candidate ẩn. Controller vẫn gọi đều để GĐ2 chỉ thay implementation.
 */
interface SuggestionEngine {
    /** Chuỗi đang composing đổi → trả danh sách gợi ý (MVP: rỗng). */
    fun onComposingChanged(composing: String): List<String>

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