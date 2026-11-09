package app.neokeyboard.ui.keyboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import app.neokeyboard.data.KeyboardPrefs
import app.neokeyboard.ime.Key
import app.neokeyboard.ime.KeyboardUiState
import app.neokeyboard.ui.design.ZeroSize
import app.neokeyboard.ui.keyboard.layouts.KeyLayouts

/**
 * Mặt bàn phím G-board parity. Tách CandidateHost / KeysHost để gõ composition
 * không recompose 30 phím (chỉ dải gợi ý cập nhật khi candidates đổi).
 */
@Composable
fun KeyboardScreen(
    ui: KeyboardUiState,
    prefs: KeyboardPrefs,
    onKeyDown: (Key) -> Unit,
    onKeyUp: (Key, Boolean) -> Unit,
    onPickCandidate: (String) -> Unit,
    onKeyCancel: (Key) -> Unit = {},
    onBackspaceRepeatStart: () -> Unit = {},
    onBackspaceWordDelete: () -> Unit = {},
    onClipboard: () -> Unit = {},
    onPasteClip: (String) -> Unit = {},
    onCloseClipboard: () -> Unit = {},
    onPasteSystem: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onTriggerLongPress: () -> Unit = {},
    onPickTrigger: (String) -> Unit = {},
    onManageTriggers: () -> Unit = {},
    onCloseTriggerMenu: () -> Unit = {},
    clipboardPanel: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLow)
            .padding(
                start = ZeroSize.KeySideMargin,
                end = ZeroSize.KeySideMargin,
                bottom = ZeroSize.KeyGapV,
            )
            .testTag("keyboard"),
    ) {
        CandidateHost(
            ui = ui,
            onPickCandidate = onPickCandidate,
            onClipboard = onClipboard,
            onOpenSettings = onOpenSettings,
            onPickTrigger = onPickTrigger,
            onManageTriggers = onManageTriggers,
            onCloseTriggerMenu = onCloseTriggerMenu,
            landscape = landscape,
        )
        if (ui.clipboardOpen && clipboardPanel != null) {
            clipboardPanel()
        } else {
            KeysHost(
                ui = ui,
                prefs = prefs,
                onKeyDown = onKeyDown,
                onKeyUp = onKeyUp,
                onKeyCancel = onKeyCancel,
                onBackspaceRepeatStart = onBackspaceRepeatStart,
                onBackspaceWordDelete = onBackspaceWordDelete,
                onTriggerLongPress = onTriggerLongPress,
                landscape = landscape,
            )
        }
    }
}

@Composable
private fun CandidateHost(
    ui: KeyboardUiState,
    onPickCandidate: (String) -> Unit,
    onClipboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickTrigger: (String) -> Unit,
    onManageTriggers: () -> Unit,
    onCloseTriggerMenu: () -> Unit,
    landscape: Boolean,
) {
    // Chỉ composable này đọc candidates/password/clipboard: gõ chữ không recompose các phím.
    // Ô mật khẩu vẫn giữ dải (không gợi ý, không clipboard) để còn nút ⚙ và bàn phím không nhảy.
    when {
        ui.clipboardOpen -> Unit
        ui.triggerMenuOpen -> TriggerStrip(
            items = ui.triggerItems,
            onPick = onPickTrigger,
            onManage = onManageTriggers,
            onClose = onCloseTriggerMenu,
            landscape = landscape,
        )
        else -> CandidateStrip(
            if (ui.passwordField) emptyList() else ui.candidates,
            onPickCandidate,
            onClipboard = onClipboard,
            onSettings = onOpenSettings,
            showClipboard = !ui.passwordField,
            landscape = landscape,
        )
    }
}

@Composable
private fun KeysHost(
    ui: KeyboardUiState,
    prefs: KeyboardPrefs,
    onKeyDown: (Key) -> Unit,
    onKeyUp: (Key, Boolean) -> Unit,
    onKeyCancel: (Key) -> Unit,
    onBackspaceRepeatStart: () -> Unit,
    onBackspaceWordDelete: () -> Unit,
    onTriggerLongPress: () -> Unit,
    landscape: Boolean,
) {
    // Chỉ đọc page (đổi trang mới recompose) + shifted đọc trong từng NeoKey.
    // Hàng không có khoảng cách: mỗi ô chạm phủ cả nửa khe hai bên, keycap tự lùi vào bằng inset.
    val rowPitch = if (landscape) ZeroSize.KeyRowPitchLandscape else ZeroSize.KeyRowPitchPortrait
    val inset = PaddingValues(horizontal = ZeroSize.KeyGapH / 2, vertical = ZeroSize.KeyGapV / 2)
    val rows = remember(ui.page) { KeyLayouts.rows(ui.page) }
    rows.forEach { row ->
        Row(Modifier.fillMaxWidth()) {
            row.forEach { key ->
                val cell = Modifier.weight(key.weight).height(rowPitch)
                if (key.isSpacer) {
                    Spacer(cell)
                } else {
                    NeoKey(
                        key = key,
                        ui = ui,
                        popupEnabled = prefs.popupEnabled,
                        onKeyDown = onKeyDown,
                        onKeyUp = onKeyUp,
                        onKeyCancel = onKeyCancel,
                        onBackspaceRepeatStart = onBackspaceRepeatStart,
                        onBackspaceWordDelete = onBackspaceWordDelete,
                        onTriggerLongPress = onTriggerLongPress,
                        inset = inset,
                        modifier = cell,
                    )
                }
            }
        }
    }
}
