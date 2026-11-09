package app.neokeyboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import app.neokeyboard.ui.design.ZeroSize
import app.neokeyboard.ui.design.ZeroSpace

/**
 * Dải gợi ý luôn chiếm chỗ phía trên phím (SPEC §5, G-board parity: dải ~52dp→48dp).
 * Luôn hiện để bàn phím không nhảy chiều cao; ẩn chỉ khi caller yêu cầu (password).
 */
@Composable
fun CandidateStrip(
    candidates: List<String>,
    onPick: (String) -> Unit,
    onClipboard: () -> Unit = {},
    landscape: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val height = if (landscape) ZeroSize.SuggestionHeightLandscape else ZeroSize.SuggestionHeight
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .testTag("candidate-strip"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            candidates.forEach { word ->
                Box(
                    Modifier
                        .clip(CircleShape)
                        .clickable { onPick(word) }
                        .padding(horizontal = ZeroSpace.Gutter, vertical = ZeroSpace.Row)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Gợi ý: $word"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = word,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
        Box(
            Modifier
                .size(height)
                .clip(CircleShape)
                .clickable { onClipboard() }
                .semantics {
                    role = Role.Button
                    contentDescription = "Bộ nhớ tạm"
                },
            contentAlignment = Alignment.Center,
        ) {
            KeyIcon(
                KeyGlyph.CLIPBOARD,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Modifier.size(ZeroSize.IconLarge),
            )
        }
    }
}
