package app.neokeyboard.clipboard

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.neokeyboard.ui.design.ZeroRadius
import app.neokeyboard.ui.design.ZeroSize
import app.neokeyboard.ui.design.ZeroSpace
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Panel clipboard trong IME (SPEC §9.2): thay nội dung bàn phím khi mở. */
@Composable
fun ClipboardPanel(
    repo: ClipboardRepository,
    onPaste: (String) -> Unit,
    onClose: () -> Unit,
    onPasteSystem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var clips by remember { mutableStateOf(repo.list()) }
    var confirmClear by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableStateOf(0) }
    fun refresh() {
        clips = repo.list()
        refreshTick++
    }
    Column(modifier.fillMaxWidth().testTag("clipboard-panel")) {
        Row(
            Modifier.fillMaxWidth().height(ZeroSize.ClipboardRow)
                .padding(horizontal = ZeroSpace.Row),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                "←",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clip(RoundedCornerShape(ZeroRadius.Field)).clickable { onClose() }.padding(ZeroSpace.Row),
            )
            Text(
                "Bộ nhớ tạm",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Dán",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(ZeroRadius.Field)).clickable { onPasteSystem() }.padding(ZeroSpace.Row),
            )
        }
        if (clips.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(ZeroSpace.Pad)) {
                Text(
                    "Chưa có nội dung. Sao chép văn bản để dùng lại.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Dán từ hệ thống",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = ZeroSpace.Row)
                        .clip(RoundedCornerShape(ZeroRadius.Field)).clickable { onPasteSystem() },
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().height(220.dp)) {
                items(clips) { clip ->
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = ZeroSpace.Row, vertical = ZeroSpace.Seam)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(ZeroRadius.Field))
                            .clip(RoundedCornerShape(ZeroRadius.Field))
                            .clickable {
                                onPaste(clip.text)
                                refresh()
                            }
                            .padding(ZeroSpace.Row)
                            .testTag("clipboard-row"),
                    ) {
                        Text(clip.text.take(200), color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                        Text(
                            SimpleDateFormat("HH:mm · dd/MM", Locale("vi")).format(Date(clip.createdAt)) + if (clip.pinned) " · Đã ghim" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row {
                            Text(
                                if (clip.pinned) "Bỏ ghim" else "Ghim",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clip(RoundedCornerShape(ZeroRadius.Field)).clickable {
                                    repo.togglePin(clip.id); refresh()
                                }.padding(horizontal = ZeroSpace.Row),
                            )
                            Text(
                                "Xóa",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clip(RoundedCornerShape(ZeroRadius.Field)).clickable {
                                    repo.delete(clip.id); refresh()
                                }.padding(horizontal = ZeroSpace.Row),
                            )
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = ZeroSpace.Row, vertical = ZeroSpace.Seam)) {
            Text(
                "Lưu 30 ngày · tối đa 50 mục",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (confirmClear) {
                Text(
                    "Xóa tất cả",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clip(RoundedCornerShape(ZeroRadius.Field)).clickable {
                        repo.clearAll(); confirmClear = false; refresh()
                    }.padding(horizontal = ZeroSpace.Row),
                )
            } else {
                Text(
                    "Xóa tất cả",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(ZeroRadius.Field)).clickable { confirmClear = true }
                        .padding(horizontal = ZeroSpace.Row),
                )
            }
        }
        @Suppress("UNUSED_EXPRESSION")
        refreshTick
    }
}
