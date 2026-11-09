package app.neokeyboard.suggest

import org.junit.Assert.assertTrue
import org.junit.Test

class LexiconTest {
    private val lex = BundledLexicon.starter()

    @Test fun prefixNgu() {
        val out = lex.prefixSearch("ngư", 4)
        assertTrue(out.any { it.word == "người" })
        assertTrue(out.size <= 4)
    }

    @Test fun prefixTieng() {
        val out = lex.prefixSearch("tiếng", 4)
        assertTrue(out.any { it.word == "tiếng" })
    }

    @Test fun emptyPrefix() { assertTrue(lex.prefixSearch("", 4).isEmpty()) }

    @Test fun normalize() { assertTrue(BundledLexicon.normalize("TIẾNG") == "tiếng") }
}
