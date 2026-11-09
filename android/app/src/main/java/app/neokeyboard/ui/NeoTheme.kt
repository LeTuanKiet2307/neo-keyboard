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

/** Nguồn màu: SYSTEM_ZERO = System Zero bridge, SYSTEM = Material You, CUSTOM = seed riêng. */
enum class ColorSource { SYSTEM_ZERO, SYSTEM, CUSTOM }

/**
 * Biên giới theme của bàn phím: một bảng màu (Material You hoặc ZeroPalette), một type ramp,
 * một shape ladder, một motion budget. Màn hình đọc MaterialTheme + LocalZero*, không hardcode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoTheme(
    mode: AppearanceMode = AppearanceMode.SYSTEM,
    colorSource: ColorSource = ColorSource.SYSTEM_ZERO,
    seed: Int = ZeroPalette.DefaultSeed,
    paletteRevision: Int = 0,
    motionEnabled: Boolean = true,
    systemZero: app.neokeyboard.theme.ThemeSnapshot? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = if (colorSource == ColorSource.SYSTEM_ZERO && systemZero != null) {
        systemZero.effectiveDark(systemDark)
    } else {
        when (mode) {
            AppearanceMode.SYSTEM -> systemDark
            AppearanceMode.LIGHT -> false
            AppearanceMode.DARK -> true
        }
    }
    val scheme = remember(dark, colorSource, seed, paletteRevision, systemZero?.revision, context) {
        when {
            colorSource == ColorSource.SYSTEM_ZERO && systemZero != null -> systemZero.scheme(dark)
            colorSource == ColorSource.SYSTEM && Build.VERSION.SDK_INT >= 31 ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else -> ZeroPalette.scheme(seed, dark)
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