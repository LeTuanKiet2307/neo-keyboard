package app.neokeyboard.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Phát âm pack thu thật (SPEC §4.5). Chưa có pack ghi âm có giấy phép:
 * fallback CreamSoundPlayer (tạm thời) để không im lặng hoàn toàn.
 */
class KeySoundPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val repo = SoundPackRepository(appContext)
    private val ringer = RingerWatch(appContext)
    private val legacy = CreamSoundPlayer(appContext, ringer)
    private val pool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val pressIds = mutableListOf<Int>()
    private val releaseIds = mutableListOf<Int>()
    private val loaded = HashSet<Int>()
    private var pressIdx = 0
    private var releaseIdx = 0

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) }
        val packs = repo.listPacks()
        if (packs.isNotEmpty()) {
            val pack = packs.first()
            pack.press.forEach { f ->
                try {
                    appContext.assets.openFd("sounds/${pack.id}/$f").use { fd ->
                        pressIds.add(pool.load(fd, 1))
                    }
                } catch (_: Exception) { }
            }
            pack.release.forEach { f ->
                try {
                    appContext.assets.openFd("sounds/${pack.id}/$f").use { fd ->
                        releaseIds.add(pool.load(fd, 1))
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun playPress(enabled: Boolean, volume: Float) {
        if (pressIds.isNotEmpty()) playPool(pressIds, enabled, volume, true)
        else legacy.playPress(enabled, volume)
    }

    fun playRelease(enabled: Boolean, volume: Float) {
        if (releaseIds.isNotEmpty()) playPool(releaseIds, enabled, volume, false)
        else legacy.playRelease(enabled, volume)
    }

    private fun playPool(ids: List<Int>, enabled: Boolean, volume: Float, isPress: Boolean) {
        if (!enabled || volume <= 0f || ids.isEmpty()) return
        if (ringer.silent) return
        val idx = if (isPress) pressIdx else releaseIdx
        val id = ids[idx % ids.size]
        if (isPress) pressIdx = (pressIdx + 1) % ids.size else releaseIdx = (releaseIdx + 1) % ids.size
        if (id !in loaded) return
        val v = (volume * (if (isPress) 1f else 0.45f)).coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, 0.97f + Math.random().toFloat() * 0.06f)
    }

    fun release() {
        pool.release()
        legacy.release()
        ringer.close()
    }
}
