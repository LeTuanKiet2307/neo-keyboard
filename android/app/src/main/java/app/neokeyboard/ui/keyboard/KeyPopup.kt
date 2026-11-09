package app.neokeyboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.neokeyboard.ui.design.ZeroRadius
import app.neokeyboard.ui.design.zeroContactShadow

/**
 * Popup xem trước phím, neo phía trên phím đang nhấn. Là cửa sổ riêng (Popup) nên không bị
 * cắt bởi mép trên input view và không cần ComposeView thứ hai. Đặt bên trong Box của phím
 * nên anchorBounds chính là bounds của phím đó trong cửa sổ.
 */
@Composable
fun KeyPopup(text: String) {
    val shape = RoundedCornerShape(ZeroRadius.Field)
    Popup(
        popupPositionProvider = remember { AboveKeyPosition() },
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false,
        ),
    ) {
        Box(
            Modifier
                .zeroContactShadow(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private class AboveKeyPosition : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val y = anchorBounds.top - popupContentSize.height - 6
        return IntOffset(x, y)
    }
}