package app.neokeyboard.suggest

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.Normalizer

/** Lexicon đóng gói: đọc vi_lexicon.bin (magic NEOLEX1) hoặc fallback danh sách khởi động.
 * Tìm prefix không phân biệt dấu: so sánh dạng ascii (đ->d, bỏ dấu). */
class BundledLexicon(private val words: List<Entry>) {
    data class Entry(val word: String, val freq: Int, val ascii: String = strip(word))

    fun prefixSearch(prefix: String, limit: Int): List<Entry> {
        val q = strip(prefix)
        if (q.isEmpty()) return emptyList()
        val from = binaryLowerBound(q)
        val out = mutableListOf<Entry>()
        for (i in from until words.size) {
            val e = words[i]
            if (!e.ascii.startsWith(q)) break
            out.add(e)
            if (out.size >= limit) break
        }
        return out
    }

    private fun binaryLowerBound(q: String): Int {
        var lo = 0
        var hi = words.size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (words[mid].ascii < q) lo = mid + 1 else hi = mid
        }
        return lo
    }

    companion object {
        const val MAGIC = "NEOLEX1"

        fun fromBytes(bytes: ByteArray): BundledLexicon {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            if (buf.remaining() < 16) return starter()
            val magic = ByteArray(8)
            buf.get(magic)
            // Writer ghi 8 byte: "NEOLEX1" + 0x00.
            if (String(magic, Charsets.UTF_8).trimEnd('\u0000') != MAGIC) return starter()
            val version = buf.int
            if (version != 1) return starter()
            val count = buf.int
            val list = mutableListOf<Entry>()
            repeat(count) {
                if (buf.remaining() < 6) return@repeat
                val len = buf.short.toInt() and 0xFFFF
                if (buf.remaining() < len + 4) return@repeat
                val wb = ByteArray(len)
                buf.get(wb)
                val freq = buf.int
                val w = String(wb, Charsets.UTF_8)
                list.add(Entry(w, freq, strip(w)))
            }
            if (list.isEmpty()) return starter()
            return BundledLexicon(list.sortedBy { it.ascii })
        }

        fun starter(): BundledLexicon = BundledLexicon(
            STARTER.map { (w, f) -> Entry(w, f, strip(w)) }.sortedBy { it.ascii },
        )

        fun normalize(s: String): String =
            Normalizer.normalize(s, Normalizer.Form.NFC).lowercase()

        /** Bỏ dấu tiếng Việt + đ->d, lowercase (dùng cho tìm prefix). */
        fun strip(s: String): String {
            var out = Normalizer.normalize(s, Normalizer.Form.NFD)
            out = out.replace(Regex("\\p{Mn}"), "").replace("đ", "d").replace("Đ", "d")
            return out.lowercase()
        }

        /** Danh sách khởi động tối thiểu (~100 từ thông dụng, do nhóm tự biên soạn). */
        val STARTER: List<Pair<String, Int>> = listOf(
            "là" to 100, "của" to 99, "và" to 98, "có" to 97, "không" to 96,
            "được" to 95, "cho" to 94, "một" to 93, "những" to 92, "này" to 91,
            "thì" to 90, "mà" to 89, "để" to 88, "với" to 87, "khi" to 86,
            "tôi" to 85, "bạn" to 84, "anh" to 83, "chị" to 82, "em" to 81,
            "người" to 80, "học" to 79, "làm" to 78, "việc" to 77, "nhà" to 76,
            "trường" to 75, "tiếng" to 74, "việt" to 73,
            "ngủ" to 72, "nghi" to 71, "nghĩ" to 70, "ngon" to 69,
            "hai" to 68, "ba" to 67, "nơi" to 66, "lịch" to 65, "trình" to 64,
            "nhắn" to 63, "tin" to 62, "đấy" to 61, "đó" to 60,
            "rất" to 59, "hơn" to 58, "ít" to 57, "nhiều" to 56, "cũng" to 55,
            "cứ" to 54, "đã" to 53, "đang" to 52, "sẽ" to 51, "muốn" to 50,
            "cần" to 49, "hiểu" to 48, "logic" to 47, "hàm" to 46,
            "nữa" to 45, "giờ" to 44, "dậy" to 43,
            "tắt" to 42, "nhắc" to 41, "giữ" to 40, "nguyên" to 39,
            "bù" to 38, "đầu" to 37, "bớt" to 36, "ong" to 35,
            "xong" to 34, "rồi" to 33, "đặt" to 32, "chuông" to 31, "gọi" to 30,
            "đọc" to 29, "sơ" to 28, "lại" to 27, "chúc" to 26, "ngày" to 25,
            "mới" to 24, "tốt" to 23, "lành" to 22, "cảm" to 21, "ơn" to 20,
        ).distinctBy { it.first }
    }
}
