package app.neokeyboard.trigger

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Danh sách cấu hình nút trigger + mục đang gán cho phím. IME và màn cài đặt cùng tiến trình nên
 * dùng chung SharedPreferences + listener như KeyboardSettings.
 */
class TriggerStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    data class State(val entries: List<TriggerEntry> = emptyList(), val activeId: String? = null) {
        val active: TriggerEntry? get() = entries.firstOrNull { it.id == activeId } ?: entries.firstOrNull()
    }

    private val _state = MutableStateFlow(read())
    val state: StateFlow<State> = _state.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _state.value = read() }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun close() = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    /** Thêm mục; bí mật được mã hóa trước khi chạm đĩa. Trả false nếu mã hóa lỗi. */
    fun add(label: String, kind: TriggerKind, value: String, pressEnter: Boolean): Boolean {
        val stored = if (kind == TriggerKind.SECRET) {
            runCatching { SecretBox.encrypt(value) }.getOrNull() ?: return false
        } else value
        val entry = TriggerEntry(UUID.randomUUID().toString(), label.ifBlank { kind.label }, kind, stored, pressEnter)
        val list = _state.value.entries + entry
        prefs.edit()
            .putString(KEY_ENTRIES, TriggerCodec.encode(list))
            .apply { if (_state.value.activeId == null) putString(KEY_ACTIVE, entry.id) }
            .apply()
        return true
    }

    fun remove(id: String) {
        val list = _state.value.entries.filterNot { it.id == id }
        prefs.edit().putString(KEY_ENTRIES, TriggerCodec.encode(list)).apply {
            if (_state.value.activeId == id) putString(KEY_ACTIVE, list.firstOrNull()?.id)
        }.apply()
    }

    fun setActive(id: String) = prefs.edit().putString(KEY_ACTIVE, id).apply()

    /** Văn bản thật sẽ commit. Bí mật giải mã tại chỗ, không cache. */
    fun resolve(entry: TriggerEntry): String? = when (entry.kind) {
        TriggerKind.PHRASE -> entry.stored
        TriggerKind.SECRET -> SecretBox.decrypt(entry.stored)
    }

    private fun read() = State(
        entries = TriggerCodec.decode(prefs.getString(KEY_ENTRIES, null)),
        activeId = prefs.getString(KEY_ACTIVE, null),
    )

    private companion object {
        const val NAME = "neo_triggers"
        const val KEY_ENTRIES = "entries"
        const val KEY_ACTIVE = "active"
    }
}
