package app.neokeyboard.telex

import org.junit.Assert.assertEquals
import org.junit.Test

class TelexEngineTest {
    private fun type(keys: String): String {
        val e = TelexEngine()
        keys.forEach { e.append(it) }
        return e.composing
    }

    @Test fun tones() {
        assertEquals("á", type("as"))
        assertEquals("à", type("af"))
        assertEquals("ả", type("ar"))
        assertEquals("ã", type("ax"))
        assertEquals("ạ", type("aj"))
    }

    @Test fun toneRemoveAndEscape() {
        assertEquals("a", type("asz"))
        assertEquals("az", type("aszz"))
    }

    @Test fun toneReplace() {
        assertEquals("mà", type("masf"))
    }

    @Test fun vowelTransforms() {
        assertEquals("â", type("aa"))
        assertEquals("ê", type("ee"))
        assertEquals("ô", type("oo"))
        assertEquals("ă", type("aw"))
        assertEquals("ơ", type("ow"))
        assertEquals("ư", type("uw"))
        assertEquals("đ", type("dd"))
        assertEquals("ư", type("w"))
    }

    @Test fun escapeOnRepeat() {
        assertEquals("aa", type("aaa"))
        assertEquals("aaa", type("aaaa"))
        assertEquals("aw", type("aww"))
        assertEquals("uw", type("uww"))
        assertEquals("dd", type("ddd"))
        assertEquals("w", type("ww"))
        assertEquals("sas", type("sass"))
    }

    @Test fun realWords() {
        assertEquals("tiếng", type("tieesng"))
        assertEquals("việt", type("vieetj"))
        assertEquals("đồng", type("ddoongf"))
        assertEquals("người", type("nguwowif"))
        assertEquals("trường", type("truwowngf"))
        assertEquals("rượu", type("ruwowuj"))
        assertEquals("múa", type("muas"))
        assertEquals("khoẻ", type("khoer"))
        assertEquals("hoà", type("hoaf"))
        assertEquals("thuỷ", type("thuyr"))
        assertEquals("quý", type("quys"))
        assertEquals("quỷ", type("quyr"))
        assertEquals("giá", type("gias"))
        assertEquals("hoạc", type("hoacj"))
    }

    @Test fun caseHandling() {
        assertEquals("Â", type("AA"))
        assertEquals("Đ", type("Dd"))
        assertEquals("Á", type("AS"))
        assertEquals("Tiếng", type("Tieesng"))
    }

    @Test fun toneKeyWithoutVowelIsLiteral() {
        assertEquals("x", type("x"))
        assertEquals("s", type("s"))
        // No vowel → tone key never transforms, so a repeat stays literal (unlike ww→w).
        assertEquals("ss", type("ss"))
    }

    @Test fun backspaceRevertsTone() {
        val e = TelexEngine()
        e.append('a'); e.append('s')
        assertEquals("á", e.composing)
        assertEquals("a", e.backspace())
        assertEquals("", e.backspace())
    }

    @Test fun backspaceRevertsTransform() {
        val e = TelexEngine()
        "ddoong".forEach { e.append(it) }
        assertEquals("đông", e.composing)
        // remove 'g' -> đôn
        assertEquals("đôn", e.backspace())
    }

    @Test fun resetClears() {
        val e = TelexEngine()
        "vietj".forEach { e.append(it) }
        e.reset()
        assertEquals("", e.composing)
        assertEquals(true, e.isEmpty)
    }

    @Test fun wAfterWholeSyllable() {
        // Lỗi người dùng báo: "suarw" từng ra "sủă".
        assertEquals("sửa", type("suarw"))
        assertEquals("sửa", type("suawr"))
        assertEquals("sửa", type("suwar"))
        assertEquals("cửa", type("cuarw"))
        assertEquals("mưa", type("muaw"))
        assertEquals("người", type("nguoiwf"))
        assertEquals("thương", type("thuongw"))
        assertEquals("mười", type("muoiwf"))
        assertEquals("gửi", type("guirw"))
        assertEquals("cứu", type("cuuws"))
        assertEquals("ơi", type("oiw"))
        assertEquals("quăn", type("quanw"))
        assertEquals("khăn", type("khanw"))
        assertEquals("hoẵng", type("hoangwx"))
        assertEquals("trường", type("truongwf"))
    }

    @Test fun wwUndoesTheRightVowel() {
        assertEquals("suaw", type("suaww"))
        assertEquals("nguoiw", type("nguoiww"))
    }
}
