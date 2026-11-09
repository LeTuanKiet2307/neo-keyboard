package app.neokeyboard.trigger

import java.util.Base64

/** Loại nội dung nút trigger: cụm từ hay gõ, hoặc bí mật (mật khẩu) được mã hóa khi lưu. */
enum class TriggerKind(val label: String) { PHRASE("Cụm từ"), SECRET("Mật khẩu") }

/**
 * Một cấu hình nút trigger. Với [TriggerKind.SECRET], [stored] là bản mã (Keystore AES-GCM),
 * không bao giờ là bản rõ; bản rõ chỉ tồn tại lúc commit vào ô nhập.
 */
data class TriggerEntry(
    val id: String,
    val label: String,
    val kind: TriggerKind,
    val stored: String,
    val pressEnter: Boolean = false,
)

/**
 * Mã hóa danh sách trigger thành văn bản cho SharedPreferences. Thuần Kotlin + java.util để
 * test JVM được (không dùng org.json). Mỗi dòng một mục, các trường Base64 ngăn bởi '.'.
 */
object TriggerCodec {
    private val enc = Base64.getUrlEncoder().withoutPadding()
    private val dec = Base64.getUrlDecoder()

    private fun e(s: String) = enc.encodeToString(s.toByteArray(Charsets.UTF_8))
    private fun d(s: String) = String(dec.decode(s), Charsets.UTF_8)

    fun encode(entries: List<TriggerEntry>): String = entries.joinToString("\n") {
        listOf(e(it.id), e(it.label), it.kind.name, e(it.stored), if (it.pressEnter) "1" else "0").joinToString(".")
    }

    fun decode(raw: String?): List<TriggerEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val f = line.split('.')
            if (f.size != 5) return@mapNotNull null
            runCatching {
                TriggerEntry(
                    id = d(f[0]),
                    label = d(f[1]),
                    kind = TriggerKind.valueOf(f[2]),
                    stored = d(f[3]),
                    pressEnter = f[4] == "1",
                )
            }.getOrNull()
        }.toList()
    }
}
