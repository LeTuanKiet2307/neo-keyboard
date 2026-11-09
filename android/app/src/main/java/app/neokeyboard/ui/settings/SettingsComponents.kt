package app.neokeyboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.neokeyboard.ui.design.ZeroRadius
import app.neokeyboard.ui.design.ZeroSpace
import app.neokeyboard.ui.keyboard.KeyGlyph
import app.neokeyboard.ui.keyboard.KeyIcon

/*
 * Khuôn chuẩn của màn cài đặt. Mọi nhóm dùng đúng ba khối: [SettingsGroup] (tiêu đề nhóm + một
 * khối bo góc), [SettingsRow] (tên + mô tả + điều khiển bên phải, cao tối thiểu 56dp) và
 * [SettingsBlock] (dòng có điều khiển rộng: chọn đoạn, thanh trượt). Không card lồng card, không
 * nút tự do giữa dòng — để mọi mục thẳng hàng, cùng lề 16dp, cùng nhịp.
 */

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ZeroSpace.Row)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = ZeroSpace.Pad),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ZeroRadius.Panel))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) { content() }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = ZeroSpace.Pad),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
fun SettingsRow(
    title: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = ZeroSpace.Pad, vertical = ZeroSpace.Gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Gutter),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) Box(contentAlignment = Alignment.Center) { trailing() }
    }
}

@Composable
fun SwitchSetting(title: String, supporting: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingsRow(title, supporting, onClick = { onChange(!checked) }) {
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Dòng mở trang con. */
@Composable
fun NavSetting(title: String, supporting: String? = null, onClick: () -> Unit) {
    SettingsRow(title, supporting, onClick = onClick) {
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsBlock(title: String, supporting: String? = null, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = ZeroSpace.Pad, vertical = ZeroSpace.Gutter),
        verticalArrangement = Arrangement.spacedBy(ZeroSpace.Row),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (supporting != null) {
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
fun ChoiceSetting(title: String, supporting: String? = null, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SettingsBlock(title, supporting) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = i == selected,
                    onClick = { onSelect(i) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    icon = {},
                ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
fun SliderSetting(title: String, value: Float, valueLabel: String, onChange: (Float) -> Unit) {
    SettingsBlock(title) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Gutter)) {
            Slider(value = value, onValueChange = onChange, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Thanh tiêu đề trang (trang con có nút quay lại). */
@Composable
fun SettingsTopBar(title: String, onBack: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = ZeroSpace.Inline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(ZeroRadius.Field)).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        } else {
            Box(Modifier.size(ZeroSpace.Gutter))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = ZeroSpace.Inline),
        )
    }
}

/** Ô báo trạng thái ở đầu trang chính (chỉ nổi màu khi cần người dùng làm gì đó). */
@Composable
fun StatusBanner(title: String, body: String, attention: Boolean, actions: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ZeroRadius.Panel))
            .background(if (attention) scheme.errorContainer else scheme.secondaryContainer)
            .padding(ZeroSpace.Pad),
        verticalArrangement = Arrangement.spacedBy(ZeroSpace.Row),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Row)) {
            KeyIcon(KeyGlyph.SETTINGS, if (attention) scheme.onErrorContainer else scheme.onSecondaryContainer, Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (attention) scheme.onErrorContainer else scheme.onSecondaryContainer)
        }
        Text(body, style = MaterialTheme.typography.bodyMedium, color = if (attention) scheme.onErrorContainer else scheme.onSecondaryContainer)
        Row(horizontalArrangement = Arrangement.spacedBy(ZeroSpace.Row)) { actions() }
    }
}
