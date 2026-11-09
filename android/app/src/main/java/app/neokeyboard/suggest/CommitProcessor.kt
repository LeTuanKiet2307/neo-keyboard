package app.neokeyboard.suggest

/**
 * Mối nối autocorrect Giai đoạn 2: biến chuỗi composing thành chuỗi commit thực tế.
 * MVP = identity (giữ nguyên). Controller gọi ngay trước khi commit một từ.
 */
fun interface CommitProcessor {
    fun process(composed: String): String

    companion object {
        val Identity = CommitProcessor { it }
    }
}