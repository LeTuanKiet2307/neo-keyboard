package app.neokeyboard.ui.keyboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * Mặt bàn phím G-board parity: dải gợi ý trên cùng + các hàng phím.
 * Số đo từ ảnh G-board trên vivo Y22s (720x1612, density 1.8): phím ~30.8dp rộng,
 * cao ~44dp, gap ~7.5dp→8dp, lề ~10.8dp→12dp, gap dọc ~8.3dp→8dp, dải ~52dp→48dp.
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
    clipboardPanel: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val keyHeight = if (landscape) ZeroSize.KeyHeightLandscape else ZeroSize.KeyHeightPortrait
    val scheme = MaterialTheme.colorScheme
    val rows = remember(ui.page) { KeyLayouts.rows(ui.page) }

    Column(
        modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLow)
            .padding(
                start = ZeroSize.KeySideMargin,
                end = ZeroSize.KeySideMargin,
                top = ZeroSize.KeyGap,
                bottom = ZeroSize.KeySideMargin,
            )
            .testTag("keyboard"),
        verticalArrangement = Arrangement.spacedBy(ZeroSize.KeyGap),
    ) {
        if (!ui.passwordField && !ui.clipboardOpen) {
            CandidateStrip(
                ui.candidates,
                onPickCandidate,
                onClipboard = onClipboard,
                landscape = landscape,
            )
        }
        if (ui.clipboardOpen && clipboardPanel != null) {
            clipboardPanel()
            return@Column
        }
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ZeroSize.KeyGap),
            ) {
                row.forEach { key ->
                    val cell = Modifier.weight(key.weight).height(keyHeight)
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
                            modifier = cell,
                        )
                    }
                }
            }
        }
    }
}
