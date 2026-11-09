package app.neokeyboard.ui.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Caelestia ladders, measured from the shell on this machine.
 *
 * One ladder for radii and one for spacing, so unrelated panels still read as one
 * product. Never introduce 6, 10, 14, 18, 24 or 36: pick the neighbouring rung.
 */
object ZeroRadius {
    /** Interior corner of a connected group: just enough to catch a highlight. */
    val Seam: Dp = 4.dp

    /** Control while pressed. Pressing shrinks the corners, it never scales. */
    val Pressed: Dp = 8.dp

    /** Fields, checked controls, compact nested rows. */
    val Field: Dp = 12.dp

    /** Default control and row radius. */
    val Row: Dp = 16.dp

    /** Control nested inside a 28 surface (28 - 8 padding). */
    val Nested: Dp = 20.dp

    /** Panels, cards, overlay surfaces. */
    val Panel: Dp = 28.dp

    /** Hero surface: clock, pinned task, call stage. */
    val Hero: Dp = 32.dp

    /** Composer pocket: concentric with its 48 circle buttons at an 8 gap (24 + 8). */
    val Pocket: Dp = 32.dp

    /** The single feature surface on a screen, if any. */
    val Feature: Dp = 48.dp
}

object ZeroSpace {
    /** Icon to label, hairline gaps. */
    val Inline: Dp = 4.dp

    /** Gap inside a row, between a label and its value. */
    val Row: Dp = 8.dp

    /** Gutter between cards on the same grid. */
    val Gutter: Dp = 12.dp

    /** Card padding, screen edge inset, field padding. */
    val Pad: Dp = 16.dp

    /** Gap between sections. */
    val Section: Dp = 20.dp

    /** Large panel padding. */
    val Panel: Dp = 28.dp

    /** Empty state breathing room. */
    val Empty: Dp = 48.dp

    /** Crease between two rows of one group: a seam, not a gap. */
    val Seam: Dp = 2.dp
}

object ZeroSize {
    /** Minimum touch target. */
    val Touch: Dp = 48.dp
    val MarkdownColumn: Dp = 144.dp
    val MessageMaxWidth: Dp = 320.dp
    val ActivityMaxWidth: Dp = 360.dp
    val ImageMaxHeight: Dp = 200.dp
    val BlockedMaxWidth: Dp = 240.dp
    val AppIcon: Dp = 40.dp
    /** Picture tile in a horizontal picker (wallpaper presets): a 9:20 phone portrait. */
    val ImageTileWidth: Dp = 56.dp
    val ImageTileHeight: Dp = 124.dp
    val AppLabelLane: Dp = 16.dp
    val ComposerButton: Dp = 56.dp
    val CompactControl: Dp = 40.dp
    val EmptyChip: Dp = 42.dp
    val Disclosure: Dp = 64.dp
    val OpticalOffset: Dp = 1.dp
    val Hairline: Dp = 1.dp
    val ContactBlur: Dp = 2.5.dp
    val PageSettle: Dp = 28.dp
    val PreviewControl: Dp = 52.dp
    /** Round remove badge on a composer thumbnail. */
    val RemoveBadge: Dp = 28.dp
    /** Số tin chưa xem cạnh Neo: viên tròn đầy 20 dp. */
    val Badge: Dp = 20.dp
    /** Progress ring that sweeps around Neo while it is held for its menu. */
    val HoldRing: Dp = 3.dp
    /** Dây nối từ nút gửi tới ngón tay khi kéo chọn quy trình. */
    val QuickWire: Dp = 2.dp
    /** Quãng kéo lên từ nút gửi để mở lớp quy trình ngay. */
    val QuickDragOpen: Dp = 48.dp
    /** Live-work dot on a tab label: a mark of state, not a count. */
    val LiveDot: Dp = 6.dp
    val PetBreathStepMillis: Long = 83L

    val Button: Dp = 48.dp
    val IconButton: Dp = 44.dp
    /**
     * Nút việc dưới tin: hình tròn 28 (Caelestia IconButton: ceil_even(glyph + 16) với glyph nhỏ) trong vùng chạm 44,
     * nên hai nút cạnh nhau hở 16 mà vẫn dễ chạm.
     */
    val TaskButton: Dp = 28.dp
    val Field: Dp = 56.dp
    val Header: Dp = 168.dp
    /** Home hero: the tall clock plate that owns the whole upper band. */
    val Clock: Dp = 240.dp
    /** Optical drop of the clock face: the line-box descent is clipped so the
     *  digits' ink meets the footer instead of leaving a gap above Neo. */
    val ClockDrop: Dp = 16.dp
    /** The custom technical colon: square-ish dots, the seam runs between them. */
    val ClockDot: Dp = 18.dp
    /** The plate's footer: Neo stacked over settings, the focus block beside them. */
    val ClockFooter: Dp = 96.dp

    /** Cột giờ ở trang Lịch trình: đủ cho "HH:MM" tnum cỡ label 14. */
    val ScheduleTime: Dp = 48.dp

    /** Icon optical sizes: design px 20/24/32/48 divided by 1.33. */
    val IconSmall: Dp = 15.dp
    val Icon: Dp = 18.dp
    val IconLarge: Dp = 24.dp
    val IconHero: Dp = 32.dp

    val PetMini: Dp = 44.dp
    val PetSmall: Dp = 32.dp
    val PetAlert: Dp = 96.dp
    val PetStageWidth: Dp = 252.dp
    val PetStageHeight: Dp = 220.dp

    /** Floating Neo bubble and the launcher grid around it. */
    val Bubble: Dp = 64.dp
    val AppCell: Dp = 64.dp
    /** Một ô của bảng sổ dữ liệu trên trang bảng. */
    val DataCell: Dp = 120.dp
    val ChatHeader: Dp = 56.dp

    /**
     * The closed app drawer: one 64 dp Neo plus a 16 dp ring above and below, so
     * the default bubble is centred in the visible band instead of hanging from
     * its top edge. The same 96 dp band is the drawer's handle when it is open.
     */
    val DrawerPeek: Dp = 96.dp

    /** Mức vượt tối đa của thanh đáy khi nảy lên. */
    val BandOvershoot: Dp = 8.dp

    val SwitchWidth: Dp = 51.dp
    val SwitchHeight: Dp = 30.dp

    /** Chip glyph đầu hàng, kiểu Nexus: hình tròn 40. */
    val IconChip: Dp = 40.dp

    /** Thanh kéo: rãnh dày 28, tay nắm cao hơn rãnh để đọc được là vật kéo được. */
    val SliderTrack: Dp = 28.dp
    val SliderHandle: Dp = 44.dp
    val SliderHandlePressed: Dp = 48.dp
    val RingStroke: Dp = 6.dp
    val ProgressHeight: Dp = 4.dp

    /** Overlay panels sit 12 inside the screen and never fill it. */
    val OverlayInset: Dp = 20.dp
    const val OverlayHeightFraction: Float = 0.72f

    /** G-board parity (ảnh đo 720x1612, density 1.8): phím ~30.8dp rộng, cao ~44dp. */
    val KeyHeightPortrait: Dp = 44.dp
    val KeyHeightLandscape: Dp = 40.dp
    /** Gap ngang giữa phím đo ~7.5dp → 8dp; gap dọc đo ~8.3dp → 8dp. */
    val KeyGap: Dp = 8.dp
    /** Lề ngang đo ~10.8dp → 12dp (Gutter). */
    val KeySideMargin: Dp = 12.dp
    /** Thành bên keycap 3D (SPEC: 4→1dp, travel 3dp). */
    val KeyWall: Dp = 4.dp
    val KeyWallPressed: Dp = 1.dp
    val KeyTravel: Dp = 3.dp
    val KeyShadowBlur: Dp = 4.dp
    val KeyShadowOffset: Dp = 2.dp
    val SuggestionHeight: Dp = 48.dp
    val SuggestionHeightLandscape: Dp = 40.dp
    val ClipboardRow: Dp = 56.dp
}

/** Spatial moves overshoot; effects never do. */
object ZeroDuration {
    const val SpatialFast: Int = 350
    const val Spatial: Int = 500
    const val SpatialSlow: Int = 650

    const val EffectsFast: Int = 150
    const val Effects: Int = 200
    const val EffectsSlow: Int = 300

    const val Press: Int = 200
    const val KeyTravel: Int = 90
    const val IconFill: Int = 200
    const val Ripple: Int = 600
    const val ReducedFade: Int = 150

    const val Trail: Int = 750
    const val Ambient: Int = 4_000
}

object ZeroEasing {
    val SpatialFast: Easing = CubicBezierEasing(0.42f, 1.67f, 0.21f, 0.90f)
    val Spatial: Easing = CubicBezierEasing(0.38f, 1.21f, 0.22f, 1f)
    val SpatialSlow: Easing = CubicBezierEasing(0.39f, 1.29f, 0.35f, 0.98f)

    val EffectsFast: Easing = CubicBezierEasing(0.31f, 0.94f, 0.34f, 1f)
    val Effects: Easing = CubicBezierEasing(0.34f, 0.80f, 0.34f, 1f)
    val EffectsSlow: Easing = CubicBezierEasing(0.34f, 0.88f, 0.34f, 1f)

    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/**
 * The spatial curve as a spring, for hand-offs from a finger. A tween restarts from
 * rest, so a fast release first stalls and then accelerates; a spring keeps the
 * release velocity. 0.8 / 380 is M3 Expressive's default spatial spring: about
 * 500 ms to settle with a ~1.5 % overshoot, the same character as [ZeroEasing.Spatial].
 */
object ZeroSpring {
    const val SpatialDamping: Float = 0.8f
    const val SpatialStiffness: Float = 380f

    /** Critically damped: a return to the anchor page lands without an overshoot. */
    const val SettleDamping: Float = 1f

    /** Thanh đáy nảy lên khi Home trở lại: lò xo k 200 / d 16, tức tỉ số tắt dần 0,57. */
    const val BandRiseStiffness: Float = 200f
    const val BandRiseDamping: Float = 16f

    /** Settle threshold for a 0–1 page progress: under half a pixel on a phone. */
    const val ProgressThreshold: Float = 0.0005f
}

object ZeroAlpha {
    /** Nháy nền một lần khi mở trang Lịch trình tới đúng nút (thẻ "Neo đã làm"). */
    const val Highlight: Float = 0.16f
    /** State layer over a surface. Hover only happens with a pointer. */
    const val Hover: Float = 0.08f
    const val Press: Float = 0.10f

    const val DisabledFill: Float = 0.10f
    const val DisabledContent: Float = 0.38f

    const val Scrim: Float = 0.5f

    /** Empty states are a well, not an illustration. */
    const val Well: Float = 0.035f
    const val WellAccent: Float = 0.045f

    /** 1px separation between two surfaces of the same tone. */
    const val Hairline: Float = 0.08f

    /** The one drop shadow in the grammar: a soft contact shadow, never stacked. */
    const val Shadow: Float = 0.28f
    const val ContactShadow: Float = 0.7f
    /** Bóng keycap G-board: nhẹ hơn SPEC 0.22 vì G-board gần như phẳng. */
    const val KeyShadow: Float = 0.12f
    const val Feather: Float = 0.20f

    /** The toast's 1 dp semantic border: the only border in the grammar. */
    const val ToastBorder: Float = 0.30f
}

/** The window edge is the frame; these are measured join radii, not spacing. */
object ZeroFillet {
    val Frame: Dp = 25.dp
    val Panel: Dp = 20.dp
}

object ZeroDeform {
    const val Drawer: Float = 0.10f
    const val Sheet: Float = 0.10f
    const val Chat: Float = 0.10f
    const val Bubble: Float = 0.15f
}

/** Depth swaps: what recedes behind a cover scales to this and fades. */
object ZeroDepth {
    const val Privacy: Float = 0.96f
}

object ZeroElevation {
    val Card: Dp = 1.dp
    val Menu: Dp = 3.dp
    val Drag: Dp = 6.dp
    val Dialog: Dp = 8.dp
    fun level(level: Int): Dp = when (level.coerceIn(0, 5)) {
        0 -> 0.dp
        1 -> 1.dp
        2 -> 3.dp
        3 -> 6.dp
        4 -> 8.dp
        else -> 12.dp
    }
}
