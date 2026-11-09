package app.neokeyboard.telex

import java.text.Normalizer

/** Bảng ký tự và cách đặt dấu cho engine Telex (thuần JVM, không Android). */
internal object TelexRules {
    // mark: 0 none, 1 circumflex (^), 2 breve (ă), 3 horn (ơ/ư)
    const val CIRC = 1
    const val BREVE = 2
    const val HORN = 3

    fun isVowel(base: Char): Boolean = base in "aeiouy"

    /** s sắc, f huyền, r hỏi, x ngã, j nặng. */
    fun toneValue(c: Char): Int = when (c) {
        's' -> 1; 'f' -> 2; 'r' -> 3; 'x' -> 4; 'j' -> 5; else -> 0
    }

    private val toneMark = charArrayOf('\u0000', '\u0301', '\u0300', '\u0309', '\u0303', '\u0323')
    private val markMark = charArrayOf('\u0000', '\u0302', '\u0306', '\u031B')

    /** Ghép nguyên âm + dấu phụ + dấu thanh rồi chuẩn hoá NFC về ký tự tiếng Việt dựng sẵn. */
    fun render(base: Char, upper: Boolean, mark: Int, tone: Int): String {
        val b = if (upper) base.uppercaseChar() else base
        if (!isVowel(base)) return b.toString()
        val sb = StringBuilder().append(b)
        if (mark in 1..3) sb.append(markMark[mark])
        if (tone in 1..5) sb.append(toneMark[tone])
        return Normalizer.normalize(sb, Normalizer.Form.NFC)
    }
}