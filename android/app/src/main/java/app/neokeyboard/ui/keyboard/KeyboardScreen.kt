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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import app.neokeyboard.data.KeyboardPrefs
import app.neokeyboard.ime.Key
import app.neokeyboard.ime.KeyboardUiState
import app.neokeyboard.ui.design.ZeroSpace
import app.neokeyboard.ui.keyboard.layouts.KeyLayouts

/**
 * Mặt bàn phím: dải candidate (ẩn khi rỗng) + các hàng phím nổi. Nền bám palette; màu chữ/keycap
 * đọc từ MaterialTheme nên đổi theo Material You. Cao độ phím co lại khi ngang.
 */
@Composable
fun KeyboardScreen(
    ui: KeyboardUiState,
    prefs: KeyboardPrefs,
    onPress: (Key) -> Unit,
    onRelease: () -> Unit,
    onPickCandidate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val keyHeight = if (landscape) 40.dp else 52.dp
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLow)
            .padding(horizontal = ZeroSpace.Inline, vertical = ZeroSpace.Row),
        verticalArrangement = Arrangement.spacedBy(ZeroSpace.Inline),
    ) {
        CandidateStrip(ui.candidates, onPickCandidate)
        KeyLayouts.rows(ui.page).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Inline),
            ) {
                row.forEach { key ->
                    val cell = Modifier.weight(key.weight).height(keyHeight)
                    if (key.isSpacer) {
                        Spacer(cell)
                    } else {
                        NeoKey(key, ui, prefs.popupEnabled, onPress, onRelease, cell)
                    }
                }
            }
        }
    }
}