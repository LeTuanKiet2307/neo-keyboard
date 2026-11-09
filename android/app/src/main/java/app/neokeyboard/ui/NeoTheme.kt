package app.neokeyboard.ui

import android.os.Build
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.neokeyboard.data.AppearanceMode
import app.neokeyboard.ui.design.LocalZeroColors
import app.neokeyboard.ui.design.LocalZeroDark
import app.neokeyboard.ui.design.LocalZeroMotion
import app.neokeyboard.ui.design.ZeroPalette
import app.neokeyboard.ui.design.ZeroRadius
import app.neokeyboard.ui.design.ZeroTypography
import app.neokeyboard.ui.design.rememberZeroIndication
import app.neokeyboard.ui.design.rememberZeroMotion

/** Nguồn màu: SYSTEM = Material You (API 31+), CUSTOM = seed riêng qua ZeroPalette. */
enum class ColorSource { SYSTEM, CUSTOM }

/**
 * Biên giới theme của bàn phím: một bảng màu (Material You hoặc ZeroPalette), một type ramp,
 * một shape ladder, một motion budget. Màn hình đọc MaterialTheme + LocalZero*, không hardcode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoTheme(
    mode: AppearanceMode = AppearanceMode.SYSTEM,
    colorSource: ColorSource = ColorSource.SYSTEM,
    seed: Int = ZeroPalette.DefaultSeed,
    paletteRevision: Int = 0,
    motionEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dark = when (mode) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }
    val scheme = remember(dark, colorSource, seed, paletteRevision, context) {
        if (colorSource == ColorSource.SYSTEM && Build.VERSION.SDK_INT >= 31) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            ZeroPalette.scheme(seed, dark)
        }
    }
    val extras = remember(dark) { ZeroPalette.extras(dark) }
    val motion = rememberZeroMotion(motionEnabled)
    CompositionLocalProvider(
        LocalZeroDark provides dark,
        LocalZeroColors provides extras,
        LocalZeroMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ZeroTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(ZeroRadius.Seam),
                small = RoundedCornerShape(ZeroRadius.Field),
                medium = RoundedCornerShape(ZeroRadius.Row),
                large = RoundedCornerShape(ZeroRadius.Panel),
                extraLarge = RoundedCornerShape(ZeroRadius.Hero),
            ),
            content = {
                CompositionLocalProvider(
                    LocalIndication provides rememberZeroIndication(),
                    LocalRippleConfiguration provides null,
                ) { content() }
            },
        )
    }
}