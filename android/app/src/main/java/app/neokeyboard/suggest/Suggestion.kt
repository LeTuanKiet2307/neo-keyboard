package app.neokeyboard.suggest

/** Loại gợi ý (SPEC §5.3). */
enum class SuggestionKind { TYPED, COMPLETION, CORRECTION, NEXT_WORD }

/** Một gợi ý cho dải candidate. */
data class Suggestion(
    val text: String,
    val kind: SuggestionKind,
    val score: Float,
)
