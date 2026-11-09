package app.neokeyboard.suggest

import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionEngineTest {
    private val engine = LexiconSuggestionEngine(BundledLexicon.starter(), null)

    @Test fun completingEmpty() { assertTrue(engine.onComposingChanged("").isEmpty()) }
    @Test fun completingNgu() { assertTrue(engine.onComposingChanged("ngu").contains("người")) }
    @Test fun maxFour() { assertTrue(engine.onComposingChanged("n").size <= 4) }
    @Test fun noBlank() { assertTrue(engine.onComposingChanged("ngu").none { it.isBlank() || it.contains(" ") }) }
}
