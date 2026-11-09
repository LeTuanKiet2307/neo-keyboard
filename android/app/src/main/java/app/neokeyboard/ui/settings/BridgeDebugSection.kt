package app.neokeyboard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import app.neokeyboard.data.KeyboardPrefs
import app.neokeyboard.data.KeyboardSettings
import app.neokeyboard.theme.ThemeBridgeClient
import app.neokeyboard.theme.ThemeBridgeDebug
import app.neokeyboard.ui.ColorSource
import app.neokeyboard.ui.design.ZeroSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bảng Debug cầu màu System Zero → Neo Keyboard: authority đang dùng, revision, thời gian IPC
 * query, độ trễ từ lúc System Zero ghi tới lúc bàn phím nhận (qua observer), và đo lặp N lần.
 */
@Composable
fun BridgeDebugSection(prefs: KeyboardPrefs, settings: KeyboardSettings) {
    val context = LocalContext.current
    val client = remember { ThemeBridgeClient(context) }
    val last by ThemeBridgeDebug.last.collectAsState()
    val events by ThemeBridgeDebug.observerEvents.collectAsState()
    val scope = rememberCoroutineScope()
    var bench by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        if (ThemeBridgeDebug.last.value == null) {
            val probe = withContext(Dispatchers.IO) { client.load().second }
            ThemeBridgeDebug.record(probe)
        }
    }

    Column(Modifier.padding(ZeroSpace.Pad), verticalArrangement = Arrangement.spacedBy(ZeroSpace.Row)) {
        if (prefs.colorSource != ColorSource.SYSTEM_ZERO) {
            Text(
                "Bàn phím đang lấy màu từ nguồn khác; chọn System Zero để thấy màu cầu.",
                style = MaterialTheme.typography.labelMedium, color = scheme.error,
            )
        }
        val fmt = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT) }
        val p = last
        val lines = buildList {
            add("authority   " + (p?.authority ?: "—"))
            add("trạng thái  " + when {
                p == null -> "chưa đo"
                p.ok -> "OK"
                else -> "lỗi: ${p.error}"
            })
            add("revision    " + (p?.revision ?: "—"))
            add("query IPC   " + (p?.let { "%.2f ms".format(it.queryMs) } ?: "—"))
            if (p != null && p.generatedAt > 0) add("SZ ghi lúc  " + fmt.format(Date(p.generatedAt)))
            if (p != null) add("Neo nhận    " + fmt.format(Date(p.receivedAt)) + if (p.viaObserver) " (observer)" else " (mở bàn phím/đo tay)")
            add("ghi → nhận  " + (p?.publishToReceiveMs?.let { "$it ms" } ?: "— (đổi màu bên System Zero để đo)"))
            add("observer    $events lần")
        }
        Text(
            lines.joinToString("\n"),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = scheme.onSurfaceVariant,
        )
        bench?.let {
            Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = scheme.onSurface)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Row)) {
            FilledTonalButton(
                enabled = !running,
                onClick = {
                    running = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val authority = client.findAuthority()
                            val samples = (1..20).map { client.load(authority).second }
                            samples.last().let(ThemeBridgeDebug::record)
                            val ok = samples.filter { it.ok }.map { it.queryMs }.sorted()
                            if (ok.isEmpty()) "đo 20 lần: không lần nào đọc được"
                            else "đo ${ok.size}/20  min %.2f · p50 %.2f · max %.2f ms".format(ok.first(), ok[ok.size / 2], ok.last())
                        }
                        bench = result
                        running = false
                    }
                },
            ) { Text(if (running) "Đang đo…" else "Đo 20 lần") }
            OutlinedButton(onClick = {
                if (!ThemeBridgeDebug.requestResync()) {
                    scope.launch { ThemeBridgeDebug.record(withContext(Dispatchers.IO) { client.load().second }) }
                }
            }) { Text("Đồng bộ lại") }
        }
        if (prefs.colorSource != ColorSource.SYSTEM_ZERO) {
            OutlinedButton(onClick = { settings.setColorSource(ColorSource.SYSTEM_ZERO) }) { Text("Dùng màu System Zero") }
        }
    }
}
