package app.neokeyboard.sound

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat

/**
 * Cache chế độ chuông. `AudioManager.ringerMode` là một binder call sang system_server: gọi mỗi
 * lần nhấn phím cộng dồn độ trễ (đo được 0.3-2ms/lần, lâu hơn khi máy bận). Đọc một lần rồi
 * nghe broadcast RINGER_MODE_CHANGED để cập nhật.
 */
class RingerWatch(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Volatile
    var silent: Boolean = audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
        private set

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val mode = intent?.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1) ?: -1
            silent = if (mode >= 0) mode == AudioManager.RINGER_MODE_SILENT
            else audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
        }
    }

    init {
        runCatching {
            ContextCompat.registerReceiver(
                appContext, receiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
    }

    fun close() {
        runCatching { appContext.unregisterReceiver(receiver) }
    }
}
