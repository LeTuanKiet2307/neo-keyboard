package app.neokeyboard.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/** Snapshot màu System Zero (SPEC §7.4): đủ 36 role light/dark. */
data class ThemeSnapshot(
    val contract: Int,
    val revision: Long,
    val mode: String,
    val wallpaperLight: Boolean?,
    val light: Map<String, Int>,
    val dark: Map<String, Int>,
    val extrasLight: Map<String, Int>,
    val extrasDark: Map<String, Int>,
) {
    fun effectiveDark(systemDark: Boolean): Boolean = when (mode) {
        "SYSTEM" -> systemDark
        "LIGHT" -> false
        "DARK" -> true
        "WALLPAPER" -> wallpaperLight?.not() ?: systemDark
        else -> systemDark
    }

    fun scheme(dark: Boolean): ColorScheme {
        val src = if (dark) this.dark else light
        val base = if (dark) darkColorScheme() else lightColorScheme()
        fun c(key: String, fallback: Color): Color = src[key]?.let { Color(it) } ?: fallback
        return base.copy(
            primary = c("primary", base.primary),
            onPrimary = c("onPrimary", base.onPrimary),
            primaryContainer = c("primaryContainer", base.primaryContainer),
            onPrimaryContainer = c("onPrimaryContainer", base.onPrimaryContainer),
            secondary = c("secondary", base.secondary),
            onSecondary = c("onSecondary", base.onSecondary),
            secondaryContainer = c("secondaryContainer", base.secondaryContainer),
            onSecondaryContainer = c("onSecondaryContainer", base.onSecondaryContainer),
            tertiary = c("tertiary", base.tertiary),
            onTertiary = c("onTertiary", base.onTertiary),
            tertiaryContainer = c("tertiaryContainer", base.tertiaryContainer),
            onTertiaryContainer = c("onTertiaryContainer", base.onTertiaryContainer),
            background = c("background", base.background),
            onBackground = c("onBackground", base.onBackground),
            surface = c("surface", base.surface),
            onSurface = c("onSurface", base.onSurface),
            surfaceVariant = c("surfaceVariant", base.surfaceVariant),
            onSurfaceVariant = c("onSurfaceVariant", base.onSurfaceVariant),
            surfaceTint = c("surfaceTint", base.surfaceTint),
            inverseSurface = c("inverseSurface", base.inverseSurface),
            inverseOnSurface = c("inverseOnSurface", base.inverseOnSurface),
            outline = c("outline", base.outline),
            outlineVariant = c("outlineVariant", base.outlineVariant),
            scrim = c("scrim", base.scrim),
            surfaceBright = c("surfaceBright", base.surfaceBright),
            surfaceDim = c("surfaceDim", base.surfaceDim),
            surfaceContainerLowest = c("surfaceContainerLowest", base.surfaceContainerLowest),
            surfaceContainerLow = c("surfaceContainerLow", base.surfaceContainerLow),
            surfaceContainer = c("surfaceContainer", base.surfaceContainer),
            surfaceContainerHigh = c("surfaceContainerHigh", base.surfaceContainerHigh),
            surfaceContainerHighest = c("surfaceContainerHighest", base.surfaceContainerHighest),
        )
    }

    companion object {
        fun parse(json: String): ThemeSnapshot? = runCatching {
            val o = JSONObject(json)
            if (o.optInt("contract", -1) != 1) return@runCatching null
            fun mapOf(name: String): Map<String, Int> {
                val obj = o.optJSONObject(name) ?: return emptyMap()
                val m = mutableMapOf<String, Int>()
                val keys = obj.keys()
                while (keys.hasNext()) { val k = keys.next(); m[k] = obj.optInt(k, Int.MIN_VALUE) }
                return m
            }
            ThemeSnapshot(
                contract = o.optInt("contract", 1),
                revision = o.optLong("revision", 0L),
                mode = o.optString("mode", "SYSTEM"),
                wallpaperLight = if (o.has("wallpaperLight") && !o.isNull("wallpaperLight")) o.optBoolean("wallpaperLight") else null,
                light = mapOf("light"),
                dark = mapOf("dark"),
                extrasLight = mapOf("extrasLight"),
                extrasDark = mapOf("extrasDark"),
            )
        }.getOrNull()
    }
}

/** Trạng thái bridge (SPEC §7.7). */
sealed interface ThemeBridgeState {
    data object Loading : ThemeBridgeState
    data class Ready(val snapshot: ThemeSnapshot, val stale: Boolean = false) : ThemeBridgeState
    data object Unavailable : ThemeBridgeState
    data class Error(val reason: String) : ThemeBridgeState
}

/** Query provider System Zero, cache SharedPreferences, observer khi IME sống. */
class ThemeBridgeClient(context: Context) {
    companion object {
        const val URI_STRING = "content://app.systemzero.theme/snapshot"
    }

    fun cacheRead(context: Context): ThemeSnapshot? {
        val prefs = context.getSharedPreferences("theme_bridge", Context.MODE_PRIVATE)
        val json = prefs.getString("json", null) ?: return null
        return ThemeSnapshot.parse(json)
    }

    fun cacheWrite(context: Context, json: String) {
        context.getSharedPreferences("theme_bridge", Context.MODE_PRIVATE)
            .edit().putString("json", json).apply()
    }

    fun loadSnapshot(context: Context): ThemeSnapshot? {
        return try {
            val cursor = context.contentResolver.query(android.net.Uri.parse(URI_STRING), null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val col = it.getColumnIndex("json")
                    if (col >= 0 && !it.isNull(col)) ThemeSnapshot.parse(it.getString(col)) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
