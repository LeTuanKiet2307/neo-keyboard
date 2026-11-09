package app.neokeyboard.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import app.neokeyboard.data.HapticStrength

/**
 * Rung phản hồi phím. Công tắc "Rung khi nhấn" của Neo Keyboard là nguồn quyết định duy nhất.
 *
 * Không dùng usage TOUCH / performHapticFeedback: khi người dùng tắt "rung khi chạm" của hệ thống
 * (vivo/FuntouchOS mặc định tắt), VibratorManagerService bỏ mọi rung TOUCH với status
 * `ignored_for_settings` — kể cả của Gboard. Bàn phím có công tắc rung riêng nên gắn usage MEDIA
 * (cường độ theo thanh rung đa phương tiện, đang bật) để rung vẫn chạy khi người dùng đã bật trong Neo.
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

    private val effects: Map<HapticStrength, VibrationEffect> = HapticStrength.entries.associateWith { strength ->
        when (strength) {
            HapticStrength.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticStrength.MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticStrength.STRONG -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
    }

    private val vibrationAttrs: VibrationAttributes? =
        if (Build.VERSION.SDK_INT >= 33) VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA) else null

    @Suppress("DEPRECATION")
    private val audioAttrs: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** Rung một nhịp khi nhấn phím. Gọi được từ thread nền. */
    fun press(enabled: Boolean, strength: HapticStrength) {
        if (!enabled) return
        val v = vibrator ?: return
        val effect = effects.getValue(strength)
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) v.vibrate(effect, vibrationAttrs!!)
            else @Suppress("DEPRECATION") v.vibrate(effect, audioAttrs)
        }
    }

    fun close() = Unit
}
