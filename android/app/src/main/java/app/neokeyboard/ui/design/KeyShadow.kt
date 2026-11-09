package app.neokeyboard.ui.design

import android.graphics.Paint
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave

/**
 * Bóng tiếp xúc offset-0, cache thành bitmap ALPHA_8 khi outline đã ổn định 250 ms.
 * Trích từ ZeroSurface của System Zero (chỉ phần bóng, bỏ ZeroMaterial/ZeroIcon).
 * Đây là chiều sâu "phím nổi": raster một lần lúc nghỉ, không vẽ lại mỗi frame.
 */
@Composable
fun Modifier.zeroContactShadow(
    shape: Shape = RoundedCornerShape(ZeroRadius.Row),
    rasterize: Boolean = true,
): Modifier = cachedZeroShadow(shape, ZeroSize.ContactBlur, 0.dp, 0.dp, remember(rasterize) { ShadowRaster(rasterize) })

internal class ShadowRaster(val enabled: Boolean) {
    var builtAtNanos = Long.MIN_VALUE
}

internal const val SHADOW_SETTLED_NANOS = 250_000_000L

private fun Modifier.cachedZeroShadow(shape: Shape, blur: Dp, offset: Dp, spread: Dp, raster: ShadowRaster): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = when (outline) {
            is Outline.Generic -> outline.path
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        }.asAndroidPath()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            setShadowLayer(
                blur.toPx(), 0f, offset.toPx(),
                android.graphics.Color.argb((ZeroAlpha.ContactShadow * 255).toInt(), 0, 0, 0),
            )
        }
        val spreadPx = spread.toPx()
        val width = size.width
        val height = size.height
        fun shadow(native: android.graphics.Canvas) = native.withSave {
            native.clipOutPath(path)
            if (spreadPx != 0f && width > 0f && height > 0f) {
                native.scale((width + spreadPx * 2) / width, (height + spreadPx * 2) / height, width / 2, height / 2)
            }
            native.drawPath(path, paint)
        }
        val now = System.nanoTime()
        val settled = raster.enabled &&
            (raster.builtAtNanos == Long.MIN_VALUE || now - raster.builtAtNanos > SHADOW_SETTLED_NANOS)
        raster.builtAtNanos = now
        val margin = kotlin.math.ceil(blur.toPx() * 2f + kotlin.math.abs(offset.toPx()) + kotlin.math.abs(spreadPx)).toInt()
        val bitmap = if (settled && width >= 1f && height >= 1f) runCatching {
            createBitmap(
                width.toInt() + 2 * margin, height.toInt() + 2 * margin,
                android.graphics.Bitmap.Config.ALPHA_8,
            ).also { image ->
                val canvas = android.graphics.Canvas(image)
                canvas.translate(margin.toFloat(), margin.toFloat())
                shadow(canvas)
            }
        }.getOrNull() else null
        val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { color = android.graphics.Color.BLACK }
        onDrawBehind {
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                if (bitmap != null) native.drawBitmap(bitmap, -margin.toFloat(), -margin.toFloat(), bitmapPaint)
                else shadow(native)
            }
        }
    }