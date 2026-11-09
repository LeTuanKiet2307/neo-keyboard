package app.neokeyboard.ime

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lặp xóa khi giữ backspace (SPEC §8.3): ngưỡng giữ do NeoKey đảm nhiệm (300ms),
 * nhịp đầu 50ms/lần, sau mỗi 4 lần giảm 5ms tới tối thiểu 25ms (≈ Gboard khi giữ lâu).
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
            var delayMs = 50L
            while (true) {
                delay(delayMs)
                onDelete()
                onTick()
                count++
                if (count % 4 == 0 && delayMs > 25L) delayMs = (delayMs - 5L).coerceAtLeast(25L)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        isRepeating = false
    }
}
