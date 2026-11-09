package app.neokeyboard.trigger

import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerCodecTest {
    @Test
    fun roundTripKeepsUnicodeNewlinesAndDots() {
        val list = listOf(
            TriggerEntry("a", "Chào", TriggerKind.PHRASE, "Xin chào.\nCảm ơn bạn!", pressEnter = false),
            TriggerEntry("b", "Wi-Fi", TriggerKind.SECRET, "QUJD+/==", pressEnter = true),
        )
        assertEquals(list, TriggerCodec.decode(TriggerCodec.encode(list)))
    }

    @Test
    fun corruptLinesAreSkipped() {
        val good = TriggerCodec.encode(listOf(TriggerEntry("x", "l", TriggerKind.PHRASE, "v")))
        val decoded = TriggerCodec.decode("rác\n$good\nA.B.KHONG.C.0")
        assertEquals(1, decoded.size)
        assertEquals("v", decoded[0].stored)
    }

    @Test
    fun emptyInput() {
        assertEquals(emptyList<TriggerEntry>(), TriggerCodec.decode(null))
        assertEquals(emptyList<TriggerEntry>(), TriggerCodec.decode(""))
    }
}
