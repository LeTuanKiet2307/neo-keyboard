package app.neokeyboard.telex

/**
 * Engine Telex thuần Kotlin (không Android). Mô hình replay: giữ chuỗi phím ASCII thô của
 * MỘT từ đang gõ; mỗi lần thêm/xoá thì chạy lại pipeline để ra chuỗi composing. Backspace =
 * bỏ phím cuối + replay nên tự hoàn dấu. Đặt dấu tính lúc render theo cấu trúc âm tiết cuối
 * (chuẩn UniKey mặc định: oa/oe/ hở đặt ở nguyên âm thứ hai, còn lại nguyên âm đầu; có âm cuối
 * thì nguyên âm cuối; 3 nguyên âm thì nguyên âm giữa).
 */
class TelexEngine {
    private class L(var base: Char, var upper: Boolean, var mark: Int)

    private val raw = StringBuilder()

    var composing: String = ""
        private set

    val isEmpty: Boolean get() = raw.isEmpty()

    fun append(c: Char): String {
        raw.append(c)
        composing = replay()
        return composing
    }

    fun backspace(): String {
        if (raw.isNotEmpty()) raw.deleteCharAt(raw.length - 1)
        composing = replay()
        return composing
    }

    fun reset() {
        raw.setLength(0)
        composing = ""
    }

    private fun replay(): String {
        val letters = ArrayList<L>()
        var tone = 0
        var prevFired = ""
        var prev = '\u0000'
        for (idx in raw.indices) {
            val ch = raw[idx]
            val c = ch.lowercaseChar()
            val up = ch.isUpperCase()
            val same = prev != '\u0000' && prev.equals(c, ignoreCase = true)
            when {
                c in "sfrxj" -> {
                    val tv = TelexRules.toneValue(c)
                    when {
                        same && prevFired == "escape" -> { letters.add(L(c, up, 0)); prevFired = "escape" }
                        same && prevFired == "tone:$tv" -> { tone = 0; letters.add(L(c, up, 0)); prevFired = "escape" }
                        letters.any { TelexRules.isVowel(it.base) } -> { tone = tv; prevFired = "tone:$tv" }
                        else -> { letters.add(L(c, up, 0)); prevFired = "append" }
                    }
                }
                c == 'z' -> when {
                    same && prevFired == "escape" -> { letters.add(L(c, up, 0)); prevFired = "escape" }
                    same && prevFired == "remove" -> { letters.add(L(c, up, 0)); prevFired = "escape" }
                    tone != 0 -> { tone = 0; prevFired = "remove" }
                    else -> { letters.add(L(c, up, 0)); prevFired = "append" }
                }
                c == 'w' -> when {
                    same && prevFired == "escape" -> { letters.add(L('w', up, 0)); prevFired = "escape" }
                    same && (prevFired == "mark" || prevFired == "wnew") -> {
                        if (prevFired == "wnew") {
                            if (letters.isNotEmpty()) letters.removeAt(letters.lastIndex)
                        } else {
                            val vi = lastVowel(letters)
                            if (vi >= 0) letters[vi].mark = 0
                        }
                        letters.add(L('w', up, 0)); prevFired = "escape"
                    }
                    else -> {
                        val vi = lastVowel(letters)
                        when {
                            vi >= 0 && letters[vi].base == 'a' && letters[vi].mark == 0 -> { letters[vi].mark = TelexRules.BREVE; prevFired = "mark" }
                            vi >= 0 && letters[vi].base == 'o' && letters[vi].mark == 0 -> { letters[vi].mark = TelexRules.HORN; prevFired = "mark" }
                            vi >= 0 && letters[vi].base == 'u' && letters[vi].mark == 0 -> { letters[vi].mark = TelexRules.HORN; prevFired = "mark" }
                            else -> { letters.add(L('u', up, TelexRules.HORN)); prevFired = "wnew" }
                        }
                    }
                }
                c in "aeo" -> when {
                    same && prevFired == "escape" -> { letters.add(L(c, up, 0)); prevFired = "escape" }
                    same && prevFired == "circ" -> {
                        if (letters.isNotEmpty() && letters.last().mark == TelexRules.CIRC) letters.last().mark = 0
                        letters.add(L(c, up, 0)); prevFired = "escape"
                    }
                    same && prevFired == "append" && letters.isNotEmpty() && letters.last().base == c && letters.last().mark == 0 -> {
                        letters.last().mark = TelexRules.CIRC; prevFired = "circ"
                    }
                    else -> { letters.add(L(c, up, 0)); prevFired = "append" }
                }
                c == 'd' -> when {
                    same && prevFired == "escape" -> { letters.add(L('d', up, 0)); prevFired = "escape" }
                    same && prevFired == "dtrans" -> {
                        if (letters.isNotEmpty()) letters.last().base = 'd'
                        letters.add(L('d', up, 0)); prevFired = "escape"
                    }
                    same && prevFired == "append" && letters.isNotEmpty() && letters.last().base == 'd' -> {
                        letters.last().base = 'đ'; prevFired = "dtrans"
                    }
                    else -> { letters.add(L('d', up, 0)); prevFired = "append" }
                }
                else -> { letters.add(L(c, up, 0)); prevFired = "append" }
            }
            prev = c
        }
        return render(letters, tone)
    }

    private fun lastVowel(letters: List<L>): Int {
        for (i in letters.indices.reversed()) if (TelexRules.isVowel(letters[i].base)) return i
        return -1
    }

    /** Chỉ số các nguyên âm hạt nhân, bỏ 'u'/'i' khi thuộc âm đầu qu-/gi-. */
    private fun nucleus(letters: List<L>): List<Int> {
        if (letters.isEmpty()) return emptyList()
        var i = 0
        while (i < letters.size && !TelexRules.isVowel(letters[i].base)) i++
        if (i == 1 && letters.size >= 3 &&
            ((letters[0].base == 'q' && letters[1].base == 'u') || (letters[0].base == 'g' && letters[1].base == 'i')) &&
            TelexRules.isVowel(letters[2].base)
        ) {
            i = 2
        }
        val out = ArrayList<Int>()
        while (i < letters.size && TelexRules.isVowel(letters[i].base)) { out.add(i); i++ }
        return out
    }

    private fun toneTarget(letters: List<L>, nuc: List<Int>): Int {
        if (nuc.isEmpty()) return -1
        val hasCoda = nuc.last() < letters.lastIndex
        if (hasCoda) return nuc.last()
        return when (nuc.size) {
            1 -> nuc[0]
            3 -> nuc[1]
            2 -> {
                val pair = "${letters[nuc[0]].base}${letters[nuc[1]].base}"
                if (pair in OA_OE_UY) nuc[1] else nuc[0]
            }
            else -> nuc[nuc.size / 2]
        }
    }

    private fun render(letters: List<L>, tone: Int): String {
        if (letters.isEmpty()) return ""
        val target = toneTarget(letters, nucleus(letters))
        val sb = StringBuilder()
        for ((i, l) in letters.withIndex()) {
            sb.append(TelexRules.render(l.base, l.upper, l.mark, if (i == target) tone else 0))
        }
        return sb.toString()
    }

    companion object {
        private val OA_OE_UY = setOf("oa", "oe", "uy")
    }
}