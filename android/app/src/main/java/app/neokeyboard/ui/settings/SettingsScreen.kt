package app.neokeyboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.neokeyboard.data.AppearanceMode
import app.neokeyboard.data.HapticStrength
import app.neokeyboard.data.KeyboardPrefs
import app.neokeyboard.data.KeyboardSettings
import app.neokeyboard.ui.ColorSource
import app.neokeyboard.ui.design.ZeroRadius
import app.neokeyboard.ui.design.ZeroSpace

/** Màn cài đặt: cuộn dọc, mỗi nhóm một card. Đọc prefs từ StateFlow, ghi qua KeyboardSettings. */
@Composable
fun SettingsScreen(
    prefs: KeyboardPrefs,
    settings: KeyboardSettings,
    imeEnabled: Boolean,
    imeSelected: Boolean,
    onOpenImeSettings: () -> Unit,
    onShowPicker: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(ZeroSpace.Pad),
            verticalArrangement = Arrangement.spacedBy(ZeroSpace.Section),
        ) {
            Text(
                "Neo Keyboard",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SectionCard { EnableFlow(imeEnabled, imeSelected, onOpenImeSettings, onShowPicker) }
            AppearanceSection(prefs, settings)
            SoundSection(prefs, settings)
            HapticsSection(prefs, settings)
            InputSection(prefs, settings)
            SuggestionsSection(prefs, settings)
            ClipboardSection(prefs, settings)
        }
    }
}

@Composable
private fun AppearanceSection(prefs: KeyboardPrefs, settings: KeyboardSettings) {
    SectionCard {
        Column(Modifier.padding(ZeroSpace.Pad), verticalArrangement = Arrangement.spacedBy(ZeroSpace.Gutter)) {
            SectionTitle("Giao diện")
            FieldLabel("Chế độ sáng tối")
            ChipRow(
                options = AppearanceMode.entries.map { it.label },
                selectedIndex = AppearanceMode.entries.indexOf(prefs.mode),
                onSelect = { settings.setMode(AppearanceMode.entries[it]) },
            )
            FieldLabel("Nguồn màu")
            val sources = listOf(ColorSource.SYSTEM_ZERO, ColorSource.SYSTEM, ColorSource.CUSTOM)
            ChipRow(
                options = listOf("System Zero", "Hệ thống", "Tuỳ chỉnh"),
                selectedIndex = sources.indexOf(prefs.colorSource).coerceIn(0, sources.lastIndex),
                onSelect = { settings.setColorSource(sources[it]) },
            )
            if (prefs.colorSource == ColorSource.SYSTEM_ZERO) {
                Text(
                    "Theo System Zero: mở bàn phím để đồng bộ. Không thấy System Zero thì dùng màu hệ thống.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (prefs.colorSource == ColorSource.CUSTOM) {
                FieldLabel("Màu chủ đạo")
                SeedSwatches(prefs.customSeed) { settings.setCustomSeed(it) }
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(prefs.customSeed, hsv)
                Slider(
                    value = hsv[0],
                    onValueChange = { hue ->
                        settings.setCustomSeed(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.55f, 0.75f)))
                    },
                    valueRange = 0f..360f,
                )
            }
        }
    }
}

@Composable
private fun SoundSection(prefs: KeyboardPrefs, settings: KeyboardSettings) {
    SectionCard {
        Column(Modifier.padding(ZeroSpace.Pad), verticalArrangement = Arrangement.spacedBy(ZeroSpace.Gutter)) {
            SectionTitle("Âm thanh")
            SwitchRow("Âm phím", prefs.soundEnabled) { settings.setSoundEnabled(it) }
            if (prefs.soundEnabled) {
                FieldLabel("Âm lượng")
                Slider(value = prefs.soundVolume, onValueChange = { settings.setSoundVolume(it) })
                Text(
                    "Bộ âm thu thật (pack) đang chờ giấy phép; hiện dùng tạm pack cream.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HapticsSection(prefs: KeyboardPrefs, settings: KeyboardSettings) {
    SectionCard {
        Column(Modifier.padding(ZeroSpace.Pad), verticalArrangement = Arrangement.spacedBy(ZeroSpace.Gutter)) {
            SectionTitle("Rung phản hồi")
            SwitchRow("Rung khi nhấn", prefs.hapticsEnabled) { settings.setHapticsEnabled(it) }
            if (prefs.hapticsEnabled) {
                FieldLabel("Mức rung")
                ChipRow(
                    options = HapticStrength.entries.map { it.label },
                    selectedIndex = HapticStrength.entries.indexOf(prefs.hapticStrength),
                    onSelect = { settings.setHapticStrength(HapticStrength.entries[it]) },
                )
            }
        }
    }
}

@Composable
private fun InputSection(prefs: KeyboardPrefs, settings: KeyboardSettings) {
    SectionCard {
        Column(Modifier.padding(ZeroSpace.Pad), verticalArrangement = Arrangement.spacedBy(ZeroSpace.Gutter)) {
            SectionTitle("Gõ tiếng Việt")
            SwitchRow("Telex", prefs.telexEnabled) { settings.setTelexEnabled(it) }
            SwitchRow("Popup xem trước phím", prefs.popupEnabled) { settings.setPopupEnabled(it) }
            SwitchRow("Tự viết hoa đầu câu", prefs.autoCap) { settings.setAutoCap(it) }
            SwitchRow("Hai lần space = dấu chấm", prefs.doubleSpacePeriod) { settings.setDoubleSpacePeriod(it) }
        }
    }
}

@Composable
private fun SuggestionsSection(prefs: KeyboardPrefs, settings: KeyboardSettings) {
    SectionCard {
        Column(Modifier.padding(ZeroSpace.Pad), verticalArrangement = Arrangement.spacedBy(ZeroSpace.Gutter)) {
            SectionTitle("Gợi ý")
            SwitchRow("Gợi ý từ", prefs.suggestionsEnabled) { settings.setSuggestionsEnabled(it) }
            SwitchRow("Học từ đã gõ", prefs.learnWords) { settings.setLearnWords(it) }
        }
    }
}

@Composable
private fun ClipboardSection(prefs: KeyboardPrefs, settings: KeyboardSettings) {
    SectionCard {
        Column(Modifier.padding(ZeroSpace.Pad), verticalArrangement = Arrangement.spacedBy(ZeroSpace.Gutter)) {
            SectionTitle("Bộ nhớ tạm")
            SwitchRow("Bật", prefs.clipboardEnabled) { settings.setClipboardEnabled(it) }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ZeroRadius.Panel),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChipRow(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Row)) {
        options.forEachIndexed { index, label ->
            FilterChip(selected = index == selectedIndex, onClick = { onSelect(index) }, label = { Text(label) })
        }
    }
}

private val SeedPresets = listOf(
    0xFF6C7A56.toInt(), 0xFF8D6E63.toInt(), 0xFF5C6BC0.toInt(), 0xFF26A69A.toInt(),
    0xFFEF5350.toInt(), 0xFFAB47BC.toInt(), 0xFF42A5F5.toInt(), 0xFFFFA726.toInt(),
)

@Composable
private fun SeedSwatches(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Row)) {
        SeedPresets.forEach { seed ->
            val isSel = seed == selected
            Box(
                Modifier
                    .size(36.dp)
                    .background(Color(seed), CircleShape)
                    .then(
                        if (isSel) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier,
                    )
                    .clickable { onSelect(seed) },
            )
        }
    }
}