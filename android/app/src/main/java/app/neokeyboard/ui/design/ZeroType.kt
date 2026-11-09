package app.neokeyboard.ui.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.neokeyboard.R

/**
 * Nunito: humanist bo tròn, đủ bộ glyph tiếng Việt. Weight đóng băng lúc build cho APK gọn.
 */
val ZeroTextFont = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
)

/** Ramp đo từ System Zero: tương phản đến từ weight và màu, không nhảy cỡ chữ. */
val ZeroTypography = Typography(
    displayLarge = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Medium, fontSize = 64.sp, lineHeight = 62.sp, letterSpacing = (-1.5).sp, fontFeatureSettings = "tnum"),
    displayMedium = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Medium, fontSize = 56.sp, lineHeight = 54.sp, letterSpacing = (-1.2).sp, fontFeatureSettings = "tnum"),
    displaySmall = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Medium, fontSize = 48.sp, lineHeight = 46.sp, letterSpacing = (-1).sp, fontFeatureSettings = "tnum"),
    headlineLarge = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 32.sp, fontFeatureSettings = "tnum"),
    headlineSmall = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = ZeroTextFont, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp),
)