package app.neokeyboard.suggest

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Engine gợi ý completion + next-word (SPEC §5.5). Học ghi nền, tìm kiếm dùng cache. */
class LexiconSuggestionEngine(
    private val lexicon: BundledLexicon,
    private val userStore: UserLexiconStore?,
    private val learnEnabled: () -> Boolean = { true },
) : SuggestionEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastCommitted: String = ""
    private val starterNext = listOf("là", "của", "và", "có", "không", "được", "cho", "một", "những", "này", "thì", "mà", "để", "với", "khi")

    override fun onComposingChanged(composing: String): List<String> {
        val raw = composing.trim()
        if (raw.isEmpty()) return emptyList()
        val q = BundledLexicon.strip(raw)
        if (q.isEmpty()) return emptyList()
        // hits từ lexicon (đã bỏ dấu) + từ người dùng có cùng prefix.
        val hits = lexicon.prefixSearch(raw, 12)
        if (hits.isEmpty()) return emptyList()
        val scored = hits.map { e ->
            val boost = if ((userStore?.wordFreq(BundledLexicon.normalize(e.word)) ?: 0) > 0) 3f else 1f
            e.word to (e.freq * boost)
        }.sortedByDescending { it.second }
        val out = mutableListOf<String>()
        // Khớp chính xác (dạng hiển thị) lên đầu (kiểu TYPED).
        val exact = BundledLexicon.normalize(raw)
        if (scored.any { BundledLexicon.normalize(it.first) == exact }) out.add(raw)
        scored.forEach { (w, _) -> if (w !in out && w.length <= 30 && !w.contains(" ")) out.add(w) }
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
        val prev = lastCommitted
        lastCommitted = w
        scope.launch {
            userStore?.learnWord(w, System.currentTimeMillis())
            if (prev.isNotEmpty()) userStore?.learnBigram(prev, w, System.currentTimeMillis())
        }
    }

    override fun reset() {
        lastCommitted = ""
    }
}
