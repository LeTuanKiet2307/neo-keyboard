package app.neokeyboard.ime

/** Phím chức năng; NONE = phím văn bản thường (chữ/số/dấu câu). */
/** TRIGGER: nút nhập nhanh (cụm từ / mật khẩu đã lưu), thay chỗ phím emoji cũ. */
enum class Special { NONE, SHIFT, BACKSPACE, ENTER, SPACE, TO_SYMBOLS, TO_LETTERS, TRIGGER }

/**
 * Một phím. [text] là ký tự gốc (chữ thường) nạp vào engine; phím chức năng để text rỗng.
 * [weight] là tỉ trọng chiều ngang trong hàng.
 */
data class Key(
    val text: String,
    val special: Special = Special.NONE,
    val weight: Float = 1f,
    val showPopup: Boolean = special == Special.NONE && text.length == 1,
    /**
     * Phần [weight] dành cho keycap nhìn thấy; phần dư là vùng chạm mở rộng (vd. 'a'/'l' ăn vào
     * khoảng trống hai đầu hàng giữa thay vì để khoảng đó thành vùng chết).
     */
    val visualWeight: Float = weight,
    /** Keycap nằm sát mép phải ô chạm (vùng mở rộng ở bên trái) hay ngược lại. */
    val visualAlignEnd: Boolean = false,
) {
    val isSpacer: Boolean get() = text.isEmpty() && special == Special.NONE
    val isLetter: Boolean get() = special == Special.NONE && text.length == 1 && text[0].isLetter()
}