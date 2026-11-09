package app.neokeyboard.suggest

/**
 * Mối nối Giai đoạn 2: từ điển người dùng (Room). MVP dùng [NoOpUserDictionary].
 * Password đã được controller bypass trước khi tới đây nên không bao giờ lộ vào từ điển.
 */
interface UserDictionary {
    fun learn(word: String)
    fun contains(word: String): Boolean
    fun completions(prefix: String, limit: Int): List<String>
}

object NoOpUserDictionary : UserDictionary {
    override fun learn(word: String) {}
    override fun contains(word: String): Boolean = false
    override fun completions(prefix: String, limit: Int): List<String> = emptyList()
}