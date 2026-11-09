package app.neokeyboard.suggest

/** Engine gợi ý completion + next-word (SPEC §5.5). */
class LexiconSuggestionEngine(
    private val lexicon: BundledLexicon,
    private val userStore: UserLexiconStore?,
    private val learnEnabled: () -> Boolean = { true },
) : SuggestionEngine {
    private var lastCommitted: String = ""
    private val starterNext = listOf("là", "của", "và", "có", "không", "được", "cho", "một", "những", "này", "thì", "mà", "để", "với", "khi")

    override fun onComposingChanged(composing: String): List<String> {
        val q = BundledLexicon.normalize(composing)
        if (q.isEmpty()) return emptyList()
        val hits = lexicon.prefixSearch(q, 12)
        if (hits.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val scored = hits.map { e ->
            val userBoost = if ((userStore?.wordFreq(e.word) ?: 0) > 0) 3f else 1f
            val score = e.freq * userBoost
            Triple(e.word, score, now)
        }.sortedByDescending { it.second }
        val out = mutableListOf<String>()
        // Khớp chính xác lên đầu (kiểu TYPED).
        if (scored.any { it.first == q }) out.add(q)
        scored.forEach { (w, _, _) -> if (w !in out && w.length <= 30) out.add(w) }
        return out.take(4)
    }

    override fun onSpace(limit: Int): List<String> {
        val prev = lastCommitted
        val out = mutableListOf<String>()
        if (prev.isNotEmpty() && userStore != null) {
            out.addAll(userStore.nextWords(prev, limit).map { it.first })
        }
        starterNext.forEach { if (it != prev && it !in out) out.add(it) }
        return out.take(limit.coerceIn(1, 5))
    }

    override fun onWordCommitted(word: String) {
        if (word.isEmpty() || !learnEnabled()) return
        val w = BundledLexicon.normalize(word)
        userStore?.learnWord(w, System.currentTimeMillis())
        if (lastCommitted.isNotEmpty()) {
            userStore?.learnBigram(lastCommitted, w, System.currentTimeMillis())
        }
        lastCommitted = w
    }

    override fun reset() {
        lastCommitted = ""
    }
}
