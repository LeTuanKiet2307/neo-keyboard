package app.neokeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextEditorTest {
    @Test fun graphemeSingle() { assertEquals(1, TextEditor.graphemeTailLength("é")) }
    @Test fun graphemeNfd() { assertEquals(2, TextEditor.graphemeTailLength("e\u0301")) }
    @Test fun graphemeFlag() { assertEquals(4, TextEditor.graphemeTailLength("🇻🇳")) }
    @Test fun graphemeEmpty() { assertEquals(0, TextEditor.graphemeTailLength("")) }
    @Test fun wordBasic() { assertTrue(TextEditor.wordSpanBeforeCursor("xin chào ") > 0) }
    @Test fun wordTrailingSpace() { assertEquals(7, TextEditor.wordSpanBeforeCursor("chào   ")) }
}
