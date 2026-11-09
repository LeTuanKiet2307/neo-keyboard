package app.neokeyboard.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import app.neokeyboard.R

/**
 * Âm switch "cream" tự tổng hợp (res/raw). SoundPool cho độ trễ thấp; usage SONIFICATION
 * để đi đúng đường âm phản hồi. 3 biến thể mỗi loại để tránh lặp. Bỏ qua nếu chưa load xong
 * (không block phím) và im khi máy ở chế độ im lặng.
 */
class CreamSoundPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val pool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loaded = HashSet<Int>()
    private val pressIds: IntArray
    private val releaseIds: IntArray
    private var pressNext = 0
    private var releaseNext = 0

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loaded.add(sampleId) }
        pressIds = intArrayOf(
            pool.load(appContext, R.raw.cream_press_0, 1),
            pool.load(appContext, R.raw.cream_press_1, 1),
            pool.load(appContext, R.raw.cream_press_2, 1),
        )
        releaseIds = intArrayOf(
            pool.load(appContext, R.raw.cream_release_0, 1),
            pool.load(appContext, R.raw.cream_release_1, 1),
            pool.load(appContext, R.raw.cream_release_2, 1),
        )
    }

    fun playPress(enabled: Boolean, volume: Float) = play(pressIds, enabled, volume, isPress = true)
    fun playRelease(enabled: Boolean, volume: Float) = play(releaseIds, enabled, volume, isPress = false)

    private fun play(ids: IntArray, enabled: Boolean, volume: Float, isPress: Boolean) {
        if (!enabled || volume <= 0f || ids.isEmpty()) return
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        val index = if (isPress) pressNext else releaseNext
        val id = ids[index]
        if (isPress) pressNext = (pressNext + 1) % ids.size else releaseNext = (releaseNext + 1) % ids.size
        if (id !in loaded) return
        val v = volume.coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}