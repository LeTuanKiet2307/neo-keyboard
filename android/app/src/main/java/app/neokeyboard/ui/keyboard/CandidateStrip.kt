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
import androidx.compose.ui.unit.dp
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
    onSettings: () -> Unit = {},
    showClipboard: Boolean = true,
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
        StripIconButton(KeyGlyph.SETTINGS, "Cài đặt bàn phím", height, onSettings)
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
        if (showClipboard) StripIconButton(KeyGlyph.CLIPBOARD, "Bộ nhớ tạm", height, onClipboard)
    }
}

@Composable
private fun StripIconButton(glyph: KeyGlyph, description: String, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(height)
            .clip(CircleShape)
            .clickable { onClick() }
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        KeyIcon(glyph, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(ZeroSize.IconLarge))
    }
}

/**
 * Dải chọn trigger (giữ phím ⚡). Thay chỗ dải gợi ý, cùng chiều cao → bàn phím không nhảy.
 * Chỉ hiện nhãn; mục mật khẩu có ổ khóa, không bao giờ hiện giá trị.
 */
@Composable
fun TriggerStrip(
    items: List<app.neokeyboard.ime.TriggerChip>,
    onPick: (String) -> Unit,
    onManage: () -> Unit,
    onClose: () -> Unit,
    landscape: Boolean = false,
) {
    val height = if (landscape) ZeroSize.SuggestionHeightLandscape else ZeroSize.SuggestionHeight
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(scheme.surfaceContainerLow)
            .testTag("trigger-strip"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StripIconButton(KeyGlyph.CLOSE, "Đóng", height, onClose)
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Row),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (items.isEmpty()) {
                Text(
                    "Chưa có mục nhập nhanh — bấm ⚙ để thêm",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            items.forEach { item ->
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(if (item.active) scheme.secondaryContainer else scheme.surfaceContainerHigh)
                        .clickable { onPick(item.id) }
                        .padding(horizontal = ZeroSpace.Gutter, vertical = ZeroSpace.Row)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Nhập nhanh: ${item.label}"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Row),
                ) {
                    val tint = if (item.active) scheme.onSecondaryContainer else scheme.onSurface
                    KeyIcon(if (item.secret) KeyGlyph.LOCK else KeyGlyph.BOLT, tint, Modifier.size(16.dp))
                    Text(item.label, style = MaterialTheme.typography.titleSmall, color = tint, maxLines = 1)
                }
            }
        }
        StripIconButton(KeyGlyph.SETTINGS, "Cài đặt nhập nhanh", height, onManage)
    }
}
