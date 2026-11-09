package app.neokeyboard.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSnapshotTest {
    private val json = """
        {"contract":1,"revision":7,"mode":"WALLPAPER","wallpaperLight":false,
        "light":{"primary":-16777216},"dark":{"primary":-1},
        "extrasLight":{},"extrasDark":{}}
    """.trimIndent()

    @Test fun parseOk() {
        val s = ThemeSnapshot.parse(json)!!
        assertEquals(7L, s.revision)
        assertEquals("WALLPAPER", s.mode)
    }

    @Test fun effectiveDarkWallpaper() {
        val s = ThemeSnapshot.parse(json)!!
        assertTrue(s.effectiveDark(systemDark = false))
        assertTrue(s.effectiveDark(systemDark = true))
    }

    @Test fun contractBad() { assertNull(ThemeSnapshot.parse("{\"contract\":2}")) }
    @Test fun garbage() { assertNull(ThemeSnapshot.parse("not json")) }

    @Test fun systemMode() {
        val s = ThemeSnapshot.parse(json.replace("WALLPAPER", "SYSTEM"))!!
        assertTrue(s.effectiveDark(systemDark = true))
        assertFalse(s.effectiveDark(systemDark = false))
    }
}
