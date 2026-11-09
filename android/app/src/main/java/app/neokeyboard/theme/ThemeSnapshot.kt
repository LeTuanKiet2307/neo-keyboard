package app.neokeyboard.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

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
            fun intField(name: String): Int? =
                Regex(""""$name"\s*:\s*(-?\d+)""").find(json)?.groupValues?.getOrNull(1)?.toIntOrNull()
            fun longField(name: String): Long? =
                Regex(""""$name"\s*:\s*(-?\d+)""").find(json)?.groupValues?.getOrNull(1)?.toLongOrNull()
            fun strField(name: String): String? =
                Regex(""""$name"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.getOrNull(1)
            val contract = intField("contract") ?: return@runCatching null
            if (contract != 1) return@runCatching null
            fun mapOf(name: String): Map<String, Int> {
                val block = Regex(""""$name"\s*:\s*\{([^}]*)\}""").find(json)?.groupValues?.getOrNull(1) ?: return emptyMap()
                val m = mutableMapOf<String, Int>()
                Regex(""""([A-Za-z0-9_]+)"\s*:\s*(-?\d+)""").findAll(block).forEach { mm ->
                    mm.groupValues.getOrNull(1)?.let { k ->
                        mm.groupValues.getOrNull(2)?.toIntOrNull()?.let { v -> m[k] = v }
                    }
                }
                return m
            }
            ThemeSnapshot(
                contract = contract,
                revision = longField("revision") ?: 0L,
                mode = strField("mode") ?: "SYSTEM",
                wallpaperLight = when {
                    json.contains("\"wallpaperLight\":true") -> true
                    json.contains("\"wallpaperLight\":false") -> false
                    else -> null
                },
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

/** Một lần đọc cầu: số đo để bảng Debug so tốc độ hai bên. Thời gian là ms epoch cùng máy. */
data class BridgeProbe(
    val authority: String?,
    val ok: Boolean,
    val queryMs: Double,
    val revision: Long = 0L,
    val generatedAt: Long = 0L,
    val servedAt: Long = 0L,
    val receivedAt: Long = System.currentTimeMillis(),
    val viaObserver: Boolean = false,
    val error: String? = null,
) {
    /** System Zero ghi xong → Neo Keyboard nhận được (chỉ có nghĩa khi đến từ observer). */
    val publishToReceiveMs: Long? get() = if (generatedAt > 0 && viaObserver) receivedAt - generatedAt else null
}

/**
 * Sổ đo dùng chung trong tiến trình Neo Keyboard (IME và màn cài đặt cùng process) để bảng Debug
 * thấy đúng những gì bàn phím đã nhận.
 */
object ThemeBridgeDebug {
    private val _last = kotlinx.coroutines.flow.MutableStateFlow<BridgeProbe?>(null)
    val last: kotlinx.coroutines.flow.StateFlow<BridgeProbe?> = _last
    private val _observerEvents = kotlinx.coroutines.flow.MutableStateFlow(0)
    val observerEvents: kotlinx.coroutines.flow.StateFlow<Int> = _observerEvents

    fun record(p: BridgeProbe) { _last.value = p }

    /** IME đăng ký khi sống; nút "Đồng bộ lại" ở cài đặt gọi để bàn phím đọc lại cầu ngay. */
    @Volatile
    var resyncHook: (() -> Unit)? = null
    fun requestResync(): Boolean = resyncHook?.let { it(); true } ?: false
    fun observerFired() { _observerEvents.value = _observerEvents.value + 1 }
}

/**
 * Client cầu màu công khai của System Zero. Dò lần lượt các authority (production, smoke, debug).
 * Query là IPC đồng bộ → chỉ gọi từ thread nền.
 */
class ThemeBridgeClient(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        const val PRODUCTION = "app.systemzero.theme"
        val TEST_AUTHORITIES = listOf("app.systemzero.smoke.theme", "app.systemzero.debug.theme")
        const val URI_STRING = "content://$PRODUCTION/snapshot"
        fun uriFor(authority: String): android.net.Uri = android.net.Uri.parse("content://$authority/snapshot")
    }

    /**
     * Authority đầu tiên có provider cài trên máy, null nếu không có System Zero hỗ trợ cầu.
     * Luôn ưu tiên System Zero thật (màu ảnh nền + cấu hình người dùng); bản smoke/debug chỉ dự phòng.
     */
    fun findAuthority(): String? {
        val order = listOf(PRODUCTION) + TEST_AUTHORITIES
        return order.firstOrNull { a ->
            runCatching { appContext.packageManager.resolveContentProvider(a, 0) != null }.getOrDefault(false)
        }
    }

    fun cacheRead(context: Context = appContext): ThemeSnapshot? {
        val prefs = context.getSharedPreferences("theme_bridge", Context.MODE_PRIVATE)
        val json = prefs.getString("json", null) ?: return null
        return ThemeSnapshot.parse(json)
    }

    fun cacheWrite(context: Context = appContext, json: String) {
        context.getSharedPreferences("theme_bridge", Context.MODE_PRIVATE)
            .edit().putString("json", json).apply()
    }

    fun loadSnapshot(context: Context = appContext): ThemeSnapshot? = load().first

    /** Đọc snapshot + số đo. Ghi cache khi đọc được. */
    fun load(authority: String? = findAuthority(), viaObserver: Boolean = false): Pair<ThemeSnapshot?, BridgeProbe> {
        if (authority == null) {
            return null to BridgeProbe(null, ok = false, queryMs = 0.0, error = "Không thấy System Zero có cầu màu")
        }
        val t0 = System.nanoTime()
        return try {
            appContext.contentResolver.query(uriFor(authority), null, null, null, null).use { c ->
                val ms = (System.nanoTime() - t0) / 1_000_000.0
                if (c == null || !c.moveToFirst()) {
                    return null to BridgeProbe(authority, ok = false, queryMs = ms, error = "Provider trả rỗng (mở System Zero một lần để xuất màu)")
                }
                fun long(name: String): Long = c.getColumnIndex(name).let { if (it >= 0 && !c.isNull(it)) c.getLong(it) else 0L }
                val col = c.getColumnIndex("json")
                val json = if (col >= 0 && !c.isNull(col)) c.getString(col) else null
                val snap = json?.let { ThemeSnapshot.parse(it) }
                if (json != null && snap != null) cacheWrite(appContext, json)
                snap to BridgeProbe(
                    authority, ok = snap != null, queryMs = ms,
                    revision = snap?.revision ?: long("revision"),
                    generatedAt = long("generated_at"), servedAt = long("served_at"),
                    viaObserver = viaObserver,
                    error = if (snap == null) "JSON sai contract" else null,
                )
            }
        } catch (e: Exception) {
            null to BridgeProbe(authority, ok = false, queryMs = (System.nanoTime() - t0) / 1_000_000.0, error = e.javaClass.simpleName)
        }
    }

    /** Nghe System Zero notifyChange → [onChange] chạy trên thread nền của observer. */
    fun observe(authority: String, onChange: () -> Unit): () -> Unit {
        val thread = android.os.HandlerThread("neo-theme-bridge").apply { start() }
        val observer = object : android.database.ContentObserver(android.os.Handler(thread.looper)) {
            override fun onChange(selfChange: Boolean) {
                ThemeBridgeDebug.observerFired()
                onChange()
            }
        }
        val ok = runCatching {
            appContext.contentResolver.registerContentObserver(uriFor(authority), true, observer)
        }.isSuccess
        if (!ok) thread.quitSafely()
        return {
            runCatching { appContext.contentResolver.unregisterContentObserver(observer) }
            thread.quitSafely()
        }
    }
}
