package app.neokeyboard.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import app.neokeyboard.data.HapticStrength

/**
 * Rung phản hồi phím, theo pattern WidgetHaptics của System Zero: guard hasVibrator(),
 * predefined effect. minSdk 29 nên createPredefined luôn có; VibratorManager chỉ từ 31.
 */
class KeyHaptics(context: Context) {
    private val appContext = context.applicationContext
    private val vibrator: Vibrator? = run {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        v?.takeIf { it.hasVibrator() }
    }

    /** Rung một nhịp khi nhấn phím. Tôn trọng toggle riêng và cài rung hệ thống. */
    fun press(enabled: Boolean, strength: HapticStrength) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!systemHapticsEnabled()) return
        val effect = when (strength) {
            HapticStrength.LIGHT -> VibrationEffect.EFFECT_TICK
            HapticStrength.MEDIUM -> VibrationEffect.EFFECT_CLICK
            HapticStrength.STRONG -> VibrationEffect.EFFECT_HEAVY_CLICK
        }
        v.vibrate(VibrationEffect.createPredefined(effect))
    }

    private fun systemHapticsEnabled(): Boolean =
        Settings.System.getInt(appContext.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
}