package app.neokeyboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.neokeyboard.ui.design.ZeroSpace

/**
 * Dải gợi ý (mối nối GĐ2). MVP luôn rỗng nên ẩn. Chạm một gợi ý → thay từ đang composing.
 */
@Composable
fun CandidateStrip(
    candidates: List<String>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (candidates.isEmpty()) return
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = ZeroSpace.Row),
        horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        candidates.forEach { word ->
            Text(
                text = word,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable { onPick(word) }
                    .padding(horizontal = ZeroSpace.Row, vertical = ZeroSpace.Inline),
            )
        }
    }
}