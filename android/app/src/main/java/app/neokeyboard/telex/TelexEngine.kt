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
        var lastMarked: List<Int> = emptyList()
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
                            lastMarked.forEach { letters[it].mark = 0 }
                        }
                        letters.add(L('w', up, 0)); prevFired = "escape"
                    }
                    else -> {
                        val marked = applyW(letters)
                        if (marked.isNotEmpty()) { lastMarked = marked; prevFired = "mark" }
                        else { letters.add(L('u', up, TelexRules.HORN)); prevFired = "wnew" }
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

    /**
     * Phím w đặt dấu móc/trăng lên ĐÚNG nguyên âm của vần (kiểu UniKey), không phải nguyên âm cuối:
     * 1. "uo" → "ươ" (mươi, thương, người)
     * 2. "ua" mở, không thuộc qu- → "ưa" (sửa, cửa, mưa); có âm cuối hoặc qu- → "ă" (quăn, quặc)
     * 3. a cuối vần → "ă" (hoẵng, khăn)
     * 4. o/u đầu tiên của vần → "ơ"/"ư" (ơi, gửi, cứu)
     * Trả về các chỉ số đã đánh dấu (để ww hoàn tác đúng chỗ), rỗng nếu không áp dụng được.
     */
    private fun applyW(letters: List<L>): List<Int> {
        val nuc = nucleus(letters)
        if (nuc.isEmpty()) return emptyList()
        fun b(i: Int) = letters[i].base
        fun free(i: Int) = letters[i].mark == 0
        val hasCoda = nuc.last() < letters.lastIndex
        for (k in 0 until nuc.size - 1) {
            val u = nuc[k]; val o = nuc[k + 1]
            if (b(u) == 'u' && b(o) == 'o' && (free(u) || free(o)) &&
                letters[u].mark in intArrayOf(0, TelexRules.HORN) && letters[o].mark in intArrayOf(0, TelexRules.HORN)
            ) {
                val changed = listOf(u, o).filter { free(it) }
                changed.forEach { letters[it].mark = TelexRules.HORN }
                return changed
            }
        }
        if (nuc.size >= 2 && b(nuc[0]) == 'u' && b(nuc[1]) == 'a' && !hasCoda && free(nuc[0]) && free(nuc[1])) {
            letters[nuc[0]].mark = TelexRules.HORN
            return listOf(nuc[0])
        }
        val last = nuc.last()
        if (b(last) == 'a' && free(last)) {
            letters[last].mark = TelexRules.BREVE
            return listOf(last)
        }
        nuc.firstOrNull { (b(it) == 'o' || b(it) == 'u') && free(it) }?.let {
            letters[it].mark = TelexRules.HORN
            return listOf(it)
        }
        nuc.lastOrNull { b(it) == 'a' && free(it) }?.let {
            letters[it].mark = TelexRules.BREVE
            return listOf(it)
        }
        return emptyList()
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