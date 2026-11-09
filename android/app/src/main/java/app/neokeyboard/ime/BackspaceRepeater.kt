package app.neokeyboard.ime

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lặp xóa khi giữ backspace (SPEC §8.3): 400ms đầu do NeoKey đảm nhiệm,
 * nhịp 70ms/lần, sau mỗi 5 lần giảm 5ms tới tối thiểu 35ms.
 */
class BackspaceRepeater(
    private val onDelete: () -> Unit,
    private val onTick: () -> Unit = {},
) {
    private var job: Job? = null
    var isRepeating: Boolean = false
        private set

    fun start(scope: kotlinx.coroutines.CoroutineScope) {
        if (isRepeating) return
        isRepeating = true
        job = scope.launch {
            var count = 0
            var delayMs = 70L
            while (true) {
                delay(delayMs)
                onDelete()
                onTick()
                count++
                if (count % 5 == 0 && delayMs > 35L) delayMs = (delayMs - 5L).coerceAtLeast(35L)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        isRepeating = false
    }
}
