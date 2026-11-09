package app.neokeyboard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import app.neokeyboard.trigger.TriggerKind
import app.neokeyboard.trigger.TriggerStore
import app.neokeyboard.ui.design.ZeroSpace

/**
 * Trang "Nhập nhanh" (phím ⚡ cạnh dấu phẩy). Chạm ⚡ = nhập mục đang chọn; giữ ⚡ = dải chọn mục.
 * Mật khẩu mã hóa bằng Android Keystore trước khi lưu; trang này không bao giờ hiện lại giá trị.
 */
@Composable
fun TriggerSection(state: TriggerStore.State, store: TriggerStore) {
    val scheme = MaterialTheme.colorScheme
    Text(
        "Chạm ⚡ để nhập mục đang chọn. Giữ ⚡ để chọn mục khác ngay trên bàn phím.",
        style = MaterialTheme.typography.bodyMedium,
        color = scheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = ZeroSpace.Pad),
    )

    SettingsGroup("Các mục") {
        if (state.entries.isEmpty()) {
            SettingsRow("Chưa có mục nào", "Thêm cụm từ hay gõ hoặc mật khẩu hay dùng ở dưới.")
        }
        val activeId = state.active?.id
        state.entries.forEachIndexed { i, entry ->
            if (i > 0) SettingsDivider()
            val preview = when (entry.kind) {
                TriggerKind.SECRET -> "Mật khẩu · ••••••"
                TriggerKind.PHRASE -> entry.stored.replace('\n', ' ').take(40)
            } + if (entry.pressEnter) " · rồi Enter" else ""
            Row {
                RadioButton(
                    selected = entry.id == activeId,
                    onClick = { store.setActive(entry.id) },
                    modifier = Modifier.padding(start = ZeroSpace.Row, top = ZeroSpace.Row),
                )
                Column(Modifier.weight(1f)) {
                    SettingsRow(entry.label, preview, onClick = { store.setActive(entry.id) }) {
                        TextButton(onClick = { store.remove(entry.id) }) { Text("Xóa") }
                    }
                }
            }
        }
    }

    var label by rememberSaveable { mutableStateOf("") }
    var value by remember { mutableStateOf("") } // không rememberSaveable: bí mật không vào saved state
    var kindIdx by rememberSaveable { mutableStateOf(0) }
    var pressEnter by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val kind = TriggerKind.entries[kindIdx]

    SettingsGroup("Thêm mục") {
        ChoiceSetting("Loại", null, TriggerKind.entries.map { it.label }, kindIdx) { kindIdx = it }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ZeroSpace.Pad),
            verticalArrangement = Arrangement.spacedBy(ZeroSpace.Row),
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(24) },
                label = { Text("Tên hiển thị") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = value,
                onValueChange = { value = it; error = null },
                label = { Text(if (kind == TriggerKind.SECRET) "Mật khẩu" else "Nội dung") },
                singleLine = kind == TriggerKind.SECRET,
                visualTransformation = if (kind == TriggerKind.SECRET) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = if (kind == TriggerKind.SECRET) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SwitchSetting("Nhấn Enter sau khi nhập", null, pressEnter) { pressEnter = it }
        if (kind == TriggerKind.SECRET) {
            SettingsRow(
                "Bảo mật",
                "Mã hóa AES-GCM bằng khóa trong Android Keystore, chỉ nằm trên máy; không vào bộ nhớ tạm hay bộ học từ. " +
                    "Ai cầm máy đã mở khóa vẫn bấm ⚡ được.",
            )
        }
        Column(Modifier.padding(ZeroSpace.Pad), verticalArrangement = Arrangement.spacedBy(ZeroSpace.Row)) {
            error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.error) }
            FilledTonalButton(
                enabled = value.isNotEmpty(),
                onClick = {
                    if (store.add(label.trim(), kind, value, pressEnter)) {
                        label = ""; value = ""; pressEnter = false
                    } else error = "Không mã hóa được bằng Keystore; chưa lưu."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Lưu mục") }
        }
    }
}
