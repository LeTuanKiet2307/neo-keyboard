package app.neokeyboard.ui.keyboard.layouts

import app.neokeyboard.ime.Key
import app.neokeyboard.ime.KeyboardPage
import app.neokeyboard.ime.Special

/** Bố cục QWERTY + trang số/ký hiệu. Mỗi hàng lấp đầy chiều ngang theo weight. */
object KeyLayouts {
    private fun letters(row: String) = row.map { Key(it.toString()) }
    private fun spacer(weight: Float) = Key("", weight = weight, showPopup = false)

    private val shift = Key("", Special.SHIFT, weight = 1.6f, showPopup = false)
    private val backspace = Key("", Special.BACKSPACE, weight = 1.6f, showPopup = false)
    private val enter = Key("", Special.ENTER, weight = 1.2f, showPopup = false)
    private val space = Key(" ", Special.SPACE, weight = 4.8f, showPopup = false)
    private val toSymbols = Key("?123", Special.TO_SYMBOLS, weight = 1.2f, showPopup = false)
    private val toLetters = Key("ABC", Special.TO_LETTERS, weight = 1.2f, showPopup = false)
    private val emoji = Key("😊", Special.EMOJI, weight = 1f, showPopup = false)

    private val letterRows: List<List<Key>> = listOf(
        letters("1234567890"),
        letters("qwertyuiop"),
        listOf(spacer(0.6f)) + letters("asdfghjkl") + listOf(spacer(0.6f)),
        listOf(shift) + letters("zxcvbnm") + listOf(backspace),
        listOf(toSymbols, Key(","), emoji, space, Key("."), enter),
    )

    private val symbolRows: List<List<Key>> = listOf(
        letters("1234567890"),
        letters("@#$%&*-+()"),
        listOf(toLetters) + letters("!\"':;/?") + listOf(backspace),
        listOf(Key("\\", weight = 1.2f), Key(","), emoji, space, Key("."), enter),
    )

    fun rows(page: KeyboardPage): List<List<Key>> = when (page) {
        KeyboardPage.LETTERS -> letterRows
        KeyboardPage.SYMBOLS -> symbolRows
    }
}