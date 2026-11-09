package app.neokeyboard.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.neokeyboard.data.AppearanceMode
import app.neokeyboard.data.HapticStrength
import app.neokeyboard.data.KeyboardPrefs
import app.neokeyboard.data.KeyboardSettings
import app.neokeyboard.trigger.TriggerKind
import app.neokeyboard.ui.ColorSource
import app.neokeyboard.ui.design.ZeroSpace

/** Trang trong màn cài đặt. Trang con mở bằng dòng "›", quay lại bằng nút ‹ hoặc Back. */
enum class SettingsPage { MAIN, TRIGGER, ADVANCED }

/**
 * Màn cài đặt Neo Keyboard. Trang chính chỉ gồm các nhóm theo việc người dùng nghĩ tới
 * (Gõ, Gợi ý, Nhập nhanh, Giao diện, Âm thanh & rung, Bộ nhớ tạm, Nâng cao); mục dài
 * (danh sách nhập nhanh, công cụ debug) tách sang trang con để trang chính gọn.
 */
@Composable
fun SettingsScreen(
    prefs: KeyboardPrefs,
    settings: KeyboardSettings,
    imeEnabled: Boolean,
    imeSelected: Boolean,
    onOpenImeSettings: () -> Unit,
    onShowPicker: () -> Unit,
    triggerState: app.neokeyboard.trigger.TriggerStore.State,
    triggers: app.neokeyboard.trigger.TriggerStore,
    focusSection: String? = null,
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    LaunchedEffect(focusSection) {
        if (focusSection == SettingsActivity.SECTION_TRIGGER) page = SettingsPage.TRIGGER
    }
    BackHandler(enabled = page != SettingsPage.MAIN) { page = SettingsPage.MAIN }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime))) {
            when (page) {
                SettingsPage.MAIN -> SettingsTopBar("Neo Keyboard")
                SettingsPage.TRIGGER -> SettingsTopBar("Nhập nhanh") { page = SettingsPage.MAIN }
                SettingsPage.ADVANCED -> SettingsTopBar("Nâng cao") { page = SettingsPage.MAIN }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ZeroSpace.Pad)
                    .padding(bottom = ZeroSpace.Panel),
                verticalArrangement = Arrangement.spacedBy(ZeroSpace.Section),
            ) {
                when (page) {
                    SettingsPage.MAIN -> MainPage(
                        prefs, settings, imeEnabled, imeSelected, onOpenImeSettings, onShowPicker, triggerState,
                        onOpen = { page = it },
                    )
                    SettingsPage.TRIGGER -> TriggerSection(triggerState, triggers)
                    SettingsPage.ADVANCED -> AdvancedPage(prefs, settings)
                }
            }
        }
    }
}

@Composable
private fun MainPage(
    prefs: KeyboardPrefs,
    settings: KeyboardSettings,
    imeEnabled: Boolean,
    imeSelected: Boolean,
    onOpenImeSettings: () -> Unit,
    onShowPicker: () -> Unit,
    triggerState: app.neokeyboard.trigger.TriggerStore.State,
    onOpen: (SettingsPage) -> Unit,
) {
    when {
        !imeEnabled -> StatusBanner(
            "Chưa bật Neo Keyboard",
            "Bật Neo Keyboard trong Cài đặt nhập liệu của hệ thống, rồi chọn nó làm bàn phím.",
            attention = true,
        ) { FilledTonalButton(onClick = onOpenImeSettings) { Text("Mở cài đặt nhập liệu") } }
        !imeSelected -> StatusBanner(
            "Chưa chọn Neo Keyboard",
            "Đã bật nhưng bàn phím đang dùng là bàn phím khác.",
            attention = true,
        ) { FilledTonalButton(onClick = onShowPicker) { Text("Chọn bàn phím") } }
        else -> SettingsGroup("Trạng thái") {
            SettingsRow("Đang dùng Neo Keyboard", "Mở một ô nhập bất kỳ để gõ thử.") {
                TextButton(onClick = onShowPicker) { Text("Đổi") }
            }
        }
    }

    SettingsGroup("Gõ") {
        SwitchSetting("Gõ Telex", "aa → â, dd → đ, s f r x j → dấu", prefs.telexEnabled) { settings.setTelexEnabled(it) }
        SettingsDivider()
        SwitchSetting("Tự viết hoa đầu câu", "Đầu ô nhập và sau . ! ? xuống dòng", prefs.autoCap) { settings.setAutoCap(it) }
        SettingsDivider()
        SwitchSetting("Hai lần dấu cách = dấu chấm", null, prefs.doubleSpacePeriod) { settings.setDoubleSpacePeriod(it) }
        SettingsDivider()
        SwitchSetting("Phóng to phím khi nhấn", null, prefs.popupEnabled) { settings.setPopupEnabled(it) }
    }

    SettingsGroup("Gợi ý") {
        SwitchSetting("Gợi ý từ", "Dải gợi ý phía trên bàn phím", prefs.suggestionsEnabled) { settings.setSuggestionsEnabled(it) }
        SettingsDivider()
        SwitchSetting("Học từ đã gõ", "Chỉ lưu trên máy, không học trong ô mật khẩu", prefs.learnWords) { settings.setLearnWords(it) }
    }

    SettingsGroup("Nhập nhanh") {
        val active = triggerState.active
        NavSetting(
            "Phím ⚡",
            when {
                active == null -> "Chưa có mục nào"
                active.kind == TriggerKind.SECRET -> "${triggerState.entries.size} mục · đang dùng: ${active.label} (mật khẩu)"
                else -> "${triggerState.entries.size} mục · đang dùng: ${active.label}"
            },
        ) { onOpen(SettingsPage.TRIGGER) }
    }

    SettingsGroup("Giao diện") {
        val sources = listOf(ColorSource.SYSTEM_ZERO, ColorSource.SYSTEM, ColorSource.CUSTOM)
        ChoiceSetting(
            "Nguồn màu",
            when (prefs.colorSource) {
                ColorSource.SYSTEM_ZERO -> "Theo bảng màu System Zero, tự đổi khi System Zero đổi"
                ColorSource.SYSTEM -> "Theo Material You của hệ thống"
                ColorSource.CUSTOM -> "Màu chủ đạo tự chọn"
            },
            listOf("System Zero", "Hệ thống", "Tuỳ chỉnh"),
            sources.indexOf(prefs.colorSource).coerceAtLeast(0),
        ) { settings.setColorSource(sources[it]) }
        if (prefs.colorSource != ColorSource.SYSTEM_ZERO) {
            SettingsDivider()
            ChoiceSetting(
                "Sáng / tối", null,
                AppearanceMode.entries.map { it.label },
                AppearanceMode.entries.indexOf(prefs.mode),
            ) { settings.setMode(AppearanceMode.entries[it]) }
        }
        if (prefs.colorSource == ColorSource.CUSTOM) {
            SettingsDivider()
            SettingsBlock("Màu chủ đạo") { SeedSwatches(prefs.customSeed) { settings.setCustomSeed(it) } }
        }
    }

    SettingsGroup("Âm thanh & rung") {
        SwitchSetting("Âm phím", "Tiếng switch linear", prefs.soundEnabled) { settings.setSoundEnabled(it) }
        if (prefs.soundEnabled) {
            SliderSetting("Âm lượng", prefs.soundVolume, "${(prefs.soundVolume * 100).toInt()}%") { settings.setSoundVolume(it) }
        }
        SettingsDivider()
        SwitchSetting(
            "Rung khi nhấn",
            "Chạy cả khi rung chạm của hệ thống đang tắt",
            prefs.hapticsEnabled,
        ) { settings.setHapticsEnabled(it) }
        if (prefs.hapticsEnabled) {
            ChoiceSetting(
                "Mức rung", null,
                HapticStrength.entries.map { it.label },
                HapticStrength.entries.indexOf(prefs.hapticStrength),
            ) { settings.setHapticStrength(HapticStrength.entries[it]) }
        }
    }

    SettingsGroup("Bộ nhớ tạm") {
        SwitchSetting(
            "Lưu lịch sử bộ nhớ tạm",
            "Mở bằng nút 📋 trên dải gợi ý. Không lưu nội dung từ ô mật khẩu.",
            prefs.clipboardEnabled,
        ) { settings.setClipboardEnabled(it) }
    }

    SettingsGroup("Khác") {
        NavSetting("Nâng cao", "Cách hiện từ đang gõ, kiểm tra cầu màu System Zero") { onOpen(SettingsPage.ADVANCED) }
        SettingsDivider()
        val context = LocalContext.current
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
        SettingsRow("Phiên bản", version)
    }
}

@Composable
private fun AdvancedPage(prefs: KeyboardPrefs, settings: KeyboardSettings) {
    SettingsGroup("Hiện từ đang gõ") {
        SwitchSetting(
            "Gạch chân từ đang gõ",
            "Tắt (khuyên dùng): chữ ghi thẳng vào ô, không gạch chân. Bật nếu một app nào đó gõ bị lỗi chữ.",
            prefs.composingUnderline,
        ) { settings.setComposingUnderline(it) }
    }
    SettingsGroup("Cầu màu System Zero") {
        BridgeDebugSection(prefs, settings)
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
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color(seed), CircleShape)
                    .then(
                        if (seed == selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier,
                    )
                    .clickable { onSelect(seed) },
            )
        }
    }
    Spacer(Modifier.height(2.dp))
}
