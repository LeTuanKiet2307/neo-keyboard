package app.neokeyboard.data

/** Sáng/tối: SYSTEM bám theo hệ thống. */
enum class AppearanceMode(val label: String) {
    SYSTEM("Tự động"), LIGHT("Sáng"), DARK("Tối"),
}

/** Kiểu dựng bảng màu từ seed (dùng cho màu tuỳ chỉnh). Dịu = mặc định. */
enum class PaletteStyle(val label: String) {
    SOFT("Dịu"), VIVID("Rực"), MUTED("Trầm"),
}

/** Kết quả phân tích ảnh nền; giữ để ZeroPalette biên dịch (chế độ ảnh nền để Giai đoạn 2). */
data class WallpaperColors(
    val seed: Int,
    val candidates: List<Int>,
    val meanLightness: Float,
    val meanChroma: Float,
    val light: Boolean,
)