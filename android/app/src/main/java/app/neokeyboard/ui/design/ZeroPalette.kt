package app.neokeyboard.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.neokeyboard.data.PaletteStyle
import app.neokeyboard.data.WallpaperColors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Roles Material's ColorScheme does not carry but the shell needs. */
@Immutable
data class ZeroExtraColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

/** True when the palette is dark; icons pick their GRAD-corrected face from it. */
val LocalZeroDark = staticCompositionLocalOf { true }

val LocalZeroColors = staticCompositionLocalOf {
    ZeroExtraColors(
        success = Color(0xFFB5CCBA),
        onSuccess = Color(0xFF213528),
        successContainer = Color(0xFF374B3E),
        onSuccessContainer = Color(0xFFD1E9D6),
    )
}

/**
 * Wallpaper-derived Material 3 palette with a real tonal surface ladder.
 *
 * Tones are OKLCH lightness (0..1), which keeps steps visually even and lets the
 * same ladder work in both modes. Text roles are nudged along their own tone axis
 * until they clear the contrast target, so a bright, saturated wallpaper cannot
 * produce unreadable chrome. The `on` rule is enforced everywhere: text on an
 * accent is that accent's hue at 90 % (dark) or 10 % (light) lightness — never
 * pure white or black.
 */
object ZeroPalette {
    /** Calm olive: the geometry background users see before choosing a wallpaper. */
    const val DefaultSeed: Int = 0xFF6C7A56.toInt()

    fun extras(dark: Boolean): ZeroExtraColors = if (dark) ZeroExtraColors(
        success = Color(0xFFB5CCBA),
        onSuccess = Color(0xFF213528),
        successContainer = Color(0xFF374B3E),
        onSuccessContainer = Color(0xFFD1E9D6),
    ) else ZeroExtraColors(
        success = Color(0xFF4F6354),
        onSuccess = Color(0xFFFFFFFF),
        successContainer = Color(0xFFD1E8D5),
        onSuccessContainer = Color(0xFF0C1F13),
    )

    /**
     * 1.1.0: bảng màu theo kiểu và số liệu ảnh (đặc tả §6.2). `SOFT` + `colors == null` đi đúng đường cũ.
     * Mặt nền lấy chroma từ ảnh (có trần theo kiểu); tertiary lấy ứng viên thứ hai nếu cách seed ≥ 40°.
     */
    fun scheme(seed: Int, dark: Boolean, style: PaletteStyle, colors: WallpaperColors?): ColorScheme {
        if (style == PaletteStyle.SOFT && colors == null) return scheme(seed, dark)
        val tuning = style.tuning()
        val source = srgbToOklch(seed)
        val chromatic = source.chroma >= 0.02
        val hue = if (chromatic) source.hue else 0.0
        val chroma = if (chromatic) source.chroma else 0.0
        val meanChroma = colors?.meanChroma?.toDouble() ?: chroma
        val neutralChroma = (meanChroma * tuning.neutralFactor).coerceIn(0.006, tuning.neutralMax)
        val second = colors?.candidates?.getOrNull(1)?.let { srgbToOklch(it) }
        val tertiaryHue = second?.takeIf { hueDistance(it.hue, hue) >= 30.0 }?.hue ?: ((hue + 60.0) % 360.0)

        val primary = Tonal(hue, min(chroma, tuning.primaryMax))
        val secondary = Tonal(hue, min(chroma * 0.40, tuning.secondaryMax))
        val tertiary = Tonal(tertiaryHue, min((second?.chroma ?: chroma) * 0.55, tuning.tertiaryMax))
        val neutral = Tonal(hue, neutralChroma)
        val neutralVariant = Tonal(hue, min(chroma * 0.30, tuning.variantMax))
        val error = Tonal(25.0, if (chromatic) 0.15 else 0.12)

        val surface = neutral.tone(if (dark) 0.187 else 0.980)
        val surfaceLowest = neutral.tone(if (dark) 0.164 else 1.000)
        val surfaceLow = neutral.tone(if (dark) 0.224 else 0.958)
        val surfaceContainer = neutral.tone(if (dark) 0.241 else 0.936)
        val surfaceHigh = neutral.tone(if (dark) 0.285 else 0.914)
        val surfaceHighest = neutral.tone(if (dark) 0.326 else 0.892)

        val onSurface = guard(neutral, if (dark) 0.914 else 0.240, surface, 7.0, dark)
        val onSurfaceVariant = guard(neutralVariant, if (dark) 0.827 else 0.470, surfaceContainer, 4.5, dark)

        val primaryColor = guard(primary, if (dark) 0.829 else 0.480, surfaceContainer, 4.5, dark)
        val onPrimary = guard(primary, if (dark) 0.200 else 0.980, primaryColor, 4.5, !dark)
        val primaryContainer = primary.tone(if (dark) 0.494 else 0.900)
        val onPrimaryContainer = guard(primary, if (dark) 0.897 else 0.280, primaryContainer, 4.5, dark)

        val secondaryColor = guard(secondary, if (dark) 0.820 else 0.500, surfaceContainer, 4.5, dark)
        val onSecondary = guard(secondary, if (dark) 0.200 else 0.980, secondaryColor, 4.5, !dark)
        val secondaryContainer = secondary.tone(if (dark) 0.397 else 0.910)
        val onSecondaryContainer = guard(secondary, if (dark) 0.773 else 0.280, secondaryContainer, 4.5, dark)

        val tertiaryColor = guard(tertiary, if (dark) 0.820 else 0.500, surfaceContainer, 4.5, dark)
        val onTertiary = guard(tertiary, if (dark) 0.200 else 0.980, tertiaryColor, 4.5, !dark)
        val tertiaryContainer = tertiary.tone(if (dark) 0.400 else 0.900)
        val onTertiaryContainer = guard(tertiary, if (dark) 0.790 else 0.280, tertiaryContainer, 4.5, dark)

        val errorColor = guard(error, if (dark) 0.830 else 0.550, surfaceContainer, 4.5, dark)
        val onError = guard(error, if (dark) 0.180 else 0.990, errorColor, 4.5, !dark)
        val errorContainer = error.tone(if (dark) 0.420 else 0.900)
        val onErrorContainer = guard(error, if (dark) 0.900 else 0.220, errorContainer, 4.5, dark)

        val inverseSurface = neutral.tone(if (dark) 0.914 else 0.240)
        val inverseOnSurface = neutral.tone(if (dark) 0.200 else 0.980)
        val surfaceVariant = neutralVariant.tone(if (dark) 0.380 else 0.900)

        val base = if (dark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = primaryColor,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = primary.tone(if (dark) 0.420 else 0.800),
            secondary = secondaryColor,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiaryColor,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = errorColor,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            background = surface,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = primaryColor,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            outline = neutralVariant.tone(if (dark) 0.656 else 0.520),
            outlineVariant = neutralVariant.tone(if (dark) 0.396 else 0.860),
            scrim = Color.Black,
            surfaceBright = if (dark) neutral.tone(0.360) else surface,
            surfaceDim = if (dark) surface else neutral.tone(0.900),
            surfaceContainerLowest = surfaceLowest,
            surfaceContainerLow = surfaceLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = surfaceHighest,
        )
    }

    fun scheme(seed: Int, dark: Boolean): ColorScheme {
        val source = srgbToOklch(seed)
        val chromatic = source.chroma >= 0.02
        val hue = if (chromatic) source.hue else 0.0
        val chroma = if (chromatic) source.chroma else 0.0

        val primary = Tonal(hue, min(chroma, 0.14))
        val secondary = Tonal(hue, min(chroma * 0.40, 0.055))
        val tertiary = Tonal((hue + 60.0) % 360.0, min(chroma * 0.55, 0.075))
        val neutral = Tonal(hue, min(chroma * 0.10, 0.016))
        val neutralVariant = Tonal(hue, min(chroma * 0.30, 0.036))
        val error = Tonal(25.0, if (chromatic) 0.15 else 0.12)

        // These stops are calibrated against the shell's live palette (OKLab L of the
        // measured hexes), not against CIELAB tone numbers: surface #141311 is 0.187,
        // container #211f1d is 0.241, high #2b2a27 is 0.285. Five rungs, one step apart.
        val surface = neutral.tone(if (dark) 0.187 else 0.980)
        val surfaceLowest = neutral.tone(if (dark) 0.164 else 1.000)
        val surfaceLow = neutral.tone(if (dark) 0.224 else 0.958)
        val surfaceContainer = neutral.tone(if (dark) 0.241 else 0.936)
        val surfaceHigh = neutral.tone(if (dark) 0.285 else 0.914)
        val surfaceHighest = neutral.tone(if (dark) 0.326 else 0.892)

        val onSurface = guard(neutral, if (dark) 0.914 else 0.240, surface, 7.0, dark)
        val onSurfaceVariant = guard(neutralVariant, if (dark) 0.827 else 0.470, surfaceContainer, 4.5, dark)

        val primaryColor = guard(primary, if (dark) 0.829 else 0.480, surfaceContainer, 4.5, dark)
        val onPrimary = guard(primary, if (dark) 0.200 else 0.980, primaryColor, 4.5, !dark)
        val primaryContainer = primary.tone(if (dark) 0.494 else 0.900)
        val onPrimaryContainer = guard(primary, if (dark) 0.897 else 0.280, primaryContainer, 4.5, dark)

        val secondaryColor = guard(secondary, if (dark) 0.820 else 0.500, surfaceContainer, 4.5, dark)
        val onSecondary = guard(secondary, if (dark) 0.200 else 0.980, secondaryColor, 4.5, !dark)
        val secondaryContainer = secondary.tone(if (dark) 0.397 else 0.910)
        val onSecondaryContainer = guard(secondary, if (dark) 0.773 else 0.280, secondaryContainer, 4.5, dark)

        val tertiaryColor = guard(tertiary, if (dark) 0.820 else 0.500, surfaceContainer, 4.5, dark)
        val onTertiary = guard(tertiary, if (dark) 0.200 else 0.980, tertiaryColor, 4.5, !dark)
        val tertiaryContainer = tertiary.tone(if (dark) 0.400 else 0.900)
        val onTertiaryContainer = guard(tertiary, if (dark) 0.790 else 0.280, tertiaryContainer, 4.5, dark)

        val errorColor = guard(error, if (dark) 0.830 else 0.550, surfaceContainer, 4.5, dark)
        val onError = guard(error, if (dark) 0.180 else 0.990, errorColor, 4.5, !dark)
        val errorContainer = error.tone(if (dark) 0.420 else 0.900)
        val onErrorContainer = guard(error, if (dark) 0.900 else 0.220, errorContainer, 4.5, dark)

        val inverseSurface = neutral.tone(if (dark) 0.914 else 0.240)
        val inverseOnSurface = neutral.tone(if (dark) 0.200 else 0.980)
        val surfaceVariant = neutralVariant.tone(if (dark) 0.380 else 0.900)

        val base = if (dark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = primaryColor,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = primary.tone(if (dark) 0.420 else 0.800),
            secondary = secondaryColor,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiaryColor,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            error = errorColor,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            background = surface,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = primaryColor,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            outline = neutralVariant.tone(if (dark) 0.656 else 0.520),
            outlineVariant = neutralVariant.tone(if (dark) 0.396 else 0.860),
            scrim = Color.Black,
            surfaceBright = if (dark) neutral.tone(0.360) else surface,
            surfaceDim = if (dark) surface else neutral.tone(0.900),
            surfaceContainerLowest = surfaceLowest,
            surfaceContainerLow = surfaceLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = surfaceHighest,
        )
    }

    /** WCAG contrast ratio, public so the palette can be tested without rendering. */
    fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun luminance(color: Color): Double {
        val r = linear(color.red.toDouble())
        val g = linear(color.green.toDouble())
        val b = linear(color.blue.toDouble())
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    internal fun srgbToOklch(argb: Int): Oklch {
        val r = linear(((argb shr 16) and 0xFF) / 255.0)
        val g = linear(((argb shr 8) and 0xFF) / 255.0)
        val b = linear((argb and 0xFF) / 255.0)
        val l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
        val m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
        val s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
        val l3 = cbrt(l)
        val m3 = cbrt(m)
        val s3 = cbrt(s)
        val lightness = 0.2104542553 * l3 + 0.7936177850 * m3 - 0.0040720468 * s3
        val a = 1.9779984951 * l3 - 2.4285922050 * m3 + 0.4505937099 * s3
        val bb = 0.0259040371 * l3 + 0.7827717662 * m3 - 0.8086757660 * s3
        val chroma = hypot(a, bb)
        val hue = ((Math.toDegrees(atan2(bb, a)) + 360.0) % 360.0)
        return Oklch(lightness.coerceIn(0.0, 1.0), chroma, hue)
    }

    internal data class Oklch(val lightness: Double, val chroma: Double, val hue: Double)

    private fun hueDistance(a: Double, b: Double): Double {
        val raw = abs(a - b) % 360.0
        return min(raw, 360.0 - raw)
    }

    /** OKLCH → sRGB, chroma thu vào gamut, giữ hue. Dùng chung cho màu suy ra (khối Neo). */
    internal fun oklchColor(lightness: Double, chroma: Double, hue: Double): Color =
        oklchToColor(lightness.coerceIn(0.0, 1.0), chroma, hue)
}

/** 1.1.0: hệ số mặt nền và trần chroma theo kiểu màu (đặc tả §6.2). */
private data class PaletteTuning(
    val neutralFactor: Double,
    val neutralMax: Double,
    val primaryMax: Double,
    val secondaryMax: Double,
    val tertiaryMax: Double,
    val variantMax: Double,
)

private fun PaletteStyle.tuning(): PaletteTuning = when (this) {
    PaletteStyle.SOFT -> PaletteTuning(.35, .030, .14, .055, .075, .036)
    PaletteStyle.VIVID -> PaletteTuning(.60, .050, .18, .090, .100, .055)
    PaletteStyle.MUTED -> PaletteTuning(.15, .012, .07, .030, .040, .036)
}

/** One hue/chroma family; every role is a tone of it. */
private class Tonal(private val hue: Double, private val chroma: Double) {
    fun tone(lightness: Double): Color = oklchToColor(lightness.coerceIn(0.0, 1.0), chroma, hue)
}

private fun guard(
    tonal: Tonal,
    preferred: Double,
    against: Color,
    minimum: Double,
    lighter: Boolean,
): Color {
    var level = preferred
    var result = tonal.tone(level)
    var steps = 0
    while (ZeroPalette.contrast(result, against) < minimum && steps++ < 60) {
        level += if (lighter) 0.01 else -0.01
        if (level <= 0.01 || level >= 0.995) break
        result = tonal.tone(level)
    }
    return result
}

private fun linear(value: Double): Double =
    if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)

private fun encode(value: Double): Double =
    if (value <= 0.0031308) 12.92 * value else 1.055 * value.pow(1 / 2.4) - 0.055

private fun oklchToColor(lightness: Double, chroma: Double, hueDegrees: Double): Color {
    val hue = Math.toRadians(hueDegrees)
    var c = chroma.coerceIn(0.0, 0.4)
    var rgb = oklabToLinear(lightness, c * cos(hue), c * sin(hue))
    var attempts = 0
    while ((rgb.any { it < -0.0001 || it > 1.0001 }) && attempts++ < 32) {
        c *= 0.88
        rgb = oklabToLinear(lightness, c * cos(hue), c * sin(hue))
    }
    return Color(
        red = encode(rgb[0].coerceIn(0.0, 1.0)).coerceIn(0.0, 1.0).toFloat(),
        green = encode(rgb[1].coerceIn(0.0, 1.0)).coerceIn(0.0, 1.0).toFloat(),
        blue = encode(rgb[2].coerceIn(0.0, 1.0)).coerceIn(0.0, 1.0).toFloat(),
        alpha = 1f,
    )
}

private fun oklabToLinear(l: Double, a: Double, b: Double): DoubleArray {
    val l3 = l + 0.3963377774 * a + 0.2158037573 * b
    val m3 = l - 0.1055613458 * a - 0.0638541728 * b
    val s3 = l - 0.0894841775 * a - 1.2914855480 * b
    val lv = l3 * l3 * l3
    val mv = m3 * m3 * m3
    val sv = s3 * s3 * s3
    return doubleArrayOf(
        4.0767416621 * lv - 3.3077115913 * mv + 0.2309699292 * sv,
        -1.2684380046 * lv + 2.6097574011 * mv - 0.3413193965 * sv,
        -0.0041960863 * lv - 0.7034186147 * mv + 1.7076147010 * sv,
    )
}

/** Signed distance between two tones, used by the palette tests. */
internal fun toneDistance(a: Color, b: Color): Double = abs(
    ZeroPalette.srgbToOklch(a.toArgb()).lightness - ZeroPalette.srgbToOklch(b.toArgb()).lightness,
)
