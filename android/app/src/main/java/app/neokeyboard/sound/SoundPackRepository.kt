package app.neokeyboard.sound

import android.content.Context
import org.json.JSONObject

/** Đọc pack âm thu thật từ assets/sounds/index.json (SPEC §4.3). Chưa có pack → fallback cream. */
class SoundPackRepository(private val context: Context) {
    data class Pack(
        val id: String, val name: String, val gain: Float,
        val press: List<String>, val release: List<String>,
    )

    fun listPacks(): List<Pack> {
        return try {
            val json = context.assets.open("sounds/index.json").bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            val arr = obj.optJSONArray("packs") ?: return emptyList()
            val out = mutableListOf<Pack>()
            for (i in 0 until arr.length()) {
                val id = arr.optString(i)
                val packJson = context.assets.open("sounds/$id/pack.json").bufferedReader().use { it.readText() }
                val p = JSONObject(packJson)
                if (p.optString("origin") == "synthesized") continue
                fun list(name: String): List<String> {
                    val a = p.optJSONArray(name) ?: return emptyList()
                    return (0 until a.length()).map { a.optString(it) }
                }
                out.add(Pack(id, p.optString("name", id), p.optDouble("gain", 1.0).toFloat(), list("press"), list("release")))
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }
}
