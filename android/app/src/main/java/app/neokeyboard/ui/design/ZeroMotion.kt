package app.neokeyboard.ui.design

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Motion budget for one screen.
 *
 * Geometry (travel, size, radius) follows the effect of the user's animation
 * settings: when animations are off everything snaps. Colour and opacity keep a
 * short crossfade, because a hard cut reads as a rendering bug rather than calm.
 */
@Immutable
class ZeroMotion(val enabled: Boolean) {
    fun <T> spatial(durationMillis: Int = ZeroDuration.Spatial): FiniteAnimationSpec<T> =
        if (enabled) tween(durationMillis, easing = ZeroEasing.Spatial) else snap()

    fun <T> spatialFast(durationMillis: Int = ZeroDuration.SpatialFast): FiniteAnimationSpec<T> =
        if (enabled) tween(durationMillis, easing = ZeroEasing.SpatialFast) else snap()

    fun <T> spatialSlow(durationMillis: Int = ZeroDuration.SpatialSlow): FiniteAnimationSpec<T> =
        if (enabled) tween(durationMillis, easing = ZeroEasing.SpatialSlow) else snap()

    /**
     * Spatial timing without overshoot, for bounded scrollers (the filmstrip pager).
     * A scroll that overshoots past its last page is clamped, and the return leg
     * then stops short of the page.
     */
    fun <T> spatialSettle(durationMillis: Int = ZeroDuration.Spatial): FiniteAnimationSpec<T> =
        if (enabled) tween(durationMillis, easing = ZeroEasing.Standard) else snap()

    /**
     * Spatial motion that continues a gesture: pass the finger's velocity as the
     * animation's initial velocity. Snaps when motion is off, like [spatial].
     */
    fun <T> spatialSpring(
        visibilityThreshold: T? = null,
        dampingRatio: Float = ZeroSpring.SpatialDamping,
    ): FiniteAnimationSpec<T> =
        if (enabled) spring(dampingRatio, ZeroSpring.SpatialStiffness, visibilityThreshold) else snap()

    /** Thanh đáy nảy lên: vượt một chút rồi về đúng chỗ. */
    fun <T> bandRise(): FiniteAnimationSpec<T> = if (enabled) spring(
        ZeroSpring.BandRiseDamping / (2f * kotlin.math.sqrt(ZeroSpring.BandRiseStiffness)),
        ZeroSpring.BandRiseStiffness,
    ) else snap()

    fun <T> effects(durationMillis: Int = ZeroDuration.Effects): FiniteAnimationSpec<T> =
        tween(
            if (enabled) durationMillis else ZeroDuration.ReducedFade,
            easing = if (enabled) ZeroEasing.Effects else LinearEasing,
        )

    fun <T> effectsFast(): FiniteAnimationSpec<T> = effects(ZeroDuration.EffectsFast)

    fun <T> effectsSlow(): FiniteAnimationSpec<T> = effects(ZeroDuration.EffectsSlow)

    /** Colour and opacity use the slower effects curve; geometry uses the spatial one. */
    fun <T> colour(): FiniteAnimationSpec<T> = effects(ZeroDuration.EffectsSlow)
}

val LocalStageCovered = compositionLocalOf { false }

val LocalZeroMotion = compositionLocalOf { ZeroMotion(enabled = true) }

@Composable
fun rememberZeroMotion(enabled: Boolean = true): ZeroMotion {
    val system = rememberSystemAnimationsEnabled()
    return remember(enabled, system) { ZeroMotion(enabled && system) }
}

/** Android's "remove animations" accessibility setting, observed live. */
@Composable
fun rememberSystemAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var enabled by remember(resolver) { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = read()
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return enabled
}
