package app.neokeyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TypingContextTest {
    @Test
    fun diffAppendOnly() = assertEquals(0 to "t", TypingContext.diff("vie", "viet"))

    @Test
    fun diffToneChangeRewritesTail() = assertEquals(2 to "ệt", TypingContext.diff("viet", "việt"))

    @Test
    fun diffDeleteAll() = assertEquals(3 to "", TypingContext.diff("abc", ""))

    @Test
    fun diffDoesNotSplitSurrogatePair() {
        val (del, ins) = TypingContext.diff("a😀", "a😁")
        assertEquals(2, del)
        assertEquals("😁", ins)
    }

    @Test
    fun capAtFieldStartAndAfterSentence() {
        assertTrue(TypingContext.shouldCap(""))
        assertTrue(TypingContext.shouldCap(TypingContext.append("", "Xong. ")))
        assertTrue(TypingContext.shouldCap(TypingContext.append(null, "a\n")))
        assertFalse(TypingContext.shouldCap(TypingContext.append(null, "abc ")))
        assertFalse(TypingContext.shouldCap(null))
    }

    @Test
    fun recentKeepsUnknownMarker() {
        val r = TypingContext.append("", "một câu dài hơn tám ký tự")
        assertEquals(TypingContext.UNKNOWN, r.first())
        assertEquals(8, r.length)
        assertNull(TypingContext.dropLast(TypingContext.UNKNOWN.toString() + "a"))
    }

    @Test
    fun doubleSpacePeriodNeedsLetterBeforeSpace() {
        assertTrue(TypingContext.canDoubleSpacePeriod(TypingContext.append(null, "chào ")))
        assertFalse(TypingContext.canDoubleSpacePeriod(TypingContext.append(null, "chào. ")))
        assertFalse(TypingContext.canDoubleSpacePeriod(null))
    }

    @Test
    fun trackerToleratesLateUpdates() {
        val t = CursorTracker()
        t.reset(10)
        t.ownEdit(1); t.ownEdit(1); t.ownEdit(1) // gõ nhanh 3 ký tự, app bận
        assertFalse(t.onUpdate(11, 11))
        assertFalse(t.onUpdate(12, 12))
        assertFalse(t.onUpdate(13, 13))
        assertEquals(13, t.expected)
    }

    @Test
    fun trackerDetectsUserTap() {
        val t = CursorTracker()
        t.reset(5)
        t.ownEdit(1)
        assertFalse(t.onUpdate(6, 6))
        assertTrue(t.onUpdate(2, 2))
    }

    @Test
    fun trackerSelectionIsAlwaysAJump() {
        val t = CursorTracker()
        t.reset(5)
        t.ownEdit(1)
        assertTrue(t.onUpdate(1, 4))
    }

    @Test
    fun trackerUnknownDeltaLearnsFromUpdate() {
        val t = CursorTracker()
        t.reset(5)
        t.ownEdit(null)
        assertFalse(t.onUpdate(4, 4))
        assertEquals(4, t.expected)
    }
}
