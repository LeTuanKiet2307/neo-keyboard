package app.neokeyboard.ime

/** Phím chức năng; NONE = phím văn bản thường (chữ/số/dấu câu). */
enum class Special { NONE, SHIFT, BACKSPACE, ENTER, SPACE, TO_SYMBOLS, TO_LETTERS }

/**
 * Một phím. [text] là ký tự gốc (chữ thường) nạp vào engine; phím chức năng để text rỗng.
 * [weight] là tỉ trọng chiều ngang trong hàng.
 */
data class Key(
    val text: String,
    val special: Special = Special.NONE,
    val weight: Float = 1f,
    val showPopup: Boolean = special == Special.NONE && text.length == 1,
) {
    val isSpacer: Boolean get() = text.isEmpty() && special == Special.NONE
    val isLetter: Boolean get() = special == Special.NONE && text.length == 1 && text[0].isLetter()
}