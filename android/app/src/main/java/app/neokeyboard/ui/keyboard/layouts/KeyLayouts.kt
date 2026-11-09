package app.neokeyboard.ui.keyboard.layouts

import app.neokeyboard.ime.Key
import app.neokeyboard.ime.KeyboardPage
import app.neokeyboard.ime.Special

/**
 * Bố cục QWERTY + trang số/ký hiệu. Mỗi hàng lấp đầy chiều ngang theo weight.
 *
 * Phím đổi trang (?123 ↔ ABC) nằm CÙNG một chỗ ở góc trái hàng dưới trên cả hai trang, để
 * ngón cái bấm lại đúng chỗ là quay về (trước đây ABC nằm ở hàng 3 thế chỗ shift → phải tìm).
 * Trang ký hiệu cũng đủ 5 hàng, backspace cùng vị trí, nên bàn phím không đổi chiều cao khi lật trang.
 */
object KeyLayouts {
    private fun letters(row: String) = row.map { Key(it.toString()) }
    private fun spacer(weight: Float) = Key("", weight = weight, showPopup = false)

    private val shift = Key("", Special.SHIFT, weight = 1.6f, showPopup = false)
    private val backspace = Key("", Special.BACKSPACE, weight = 1.6f, showPopup = false)
    private val enter = Key("", Special.ENTER, weight = 1.2f, showPopup = false)
    private val space = Key(" ", Special.SPACE, weight = 4.8f, showPopup = false)
    private val toSymbols = Key("?123", Special.TO_SYMBOLS, weight = 1.2f, showPopup = false)
    private val toLetters = Key("ABC", Special.TO_LETTERS, weight = 1.2f, showPopup = false)
    private val trigger = Key("", Special.TRIGGER, weight = 1f, showPopup = false)

    private val letterRows: List<List<Key>> = listOf(
        letters("1234567890"),
        letters("qwertyuiop"),
        // 'a' và 'l' phủ luôn nửa ô trống hai đầu: chạm sát mép vẫn ra chữ.
        listOf(Key("a", weight = 1.6f, visualWeight = 1f, visualAlignEnd = true)) +
            letters("sdfghjk") + listOf(Key("l", weight = 1.6f, visualWeight = 1f)),
        listOf(shift) + letters("zxcvbnm") + listOf(backspace),
        listOf(toSymbols, Key(","), trigger, space, Key("."), enter),
    )

    private val symbolRows: List<List<Key>> = listOf(
        letters("1234567890"),
        letters("@#$%&*-+()"),
        listOf(Key("_"), Key("~"), Key("\\"), Key("<"), Key(">"), Key("["), Key("]"), Key("{"), Key("}"), Key("|")),
        listOf(Key("=", weight = 1.6f)) + letters("!\"':;/?") + listOf(backspace),
        listOf(toLetters, Key(","), trigger, space, Key("."), enter),
    )

    fun rows(page: KeyboardPage): List<List<Key>> = when (page) {
        KeyboardPage.LETTERS -> letterRows
        KeyboardPage.SYMBOLS -> symbolRows
    }
}