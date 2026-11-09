package app.neokeyboard.clipboard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

/** SQLite clipboard trong IME (SPEC §9.6): 50 mục, pinned 10, dedupe SHA-256. */
class ClipboardRepository(context: Context) : SQLiteOpenHelper(
    context.applicationContext, "clipboard.db", null, 1,
) {
    data class Clip(val id: Long, val text: String, val createdAt: Long, val pinned: Boolean)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE clips (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, hash TEXT NOT NULL UNIQUE, created_at INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE INDEX clips_created ON clips(created_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) = Unit

    fun list(): List<Clip> {
        val db = readableDatabase
        db.rawQuery("SELECT id, text, created_at, pinned FROM clips ORDER BY pinned DESC, created_at DESC LIMIT 60", null).use { c ->
            val out = mutableListOf<Clip>()
            while (c.moveToNext()) out.add(Clip(c.getLong(0), c.getString(1), c.getLong(2), c.getInt(3) == 1))
            return out
        }
    }

    fun add(text: String, now: Long = System.currentTimeMillis()): Boolean {
        val t = text.trim()
        if (t.isEmpty() || t.length > 100_000) return false
        val hash = sha256(t)
        val db = writableDatabase
        val v = ContentValues().apply {
            put("text", t); put("hash", hash); put("created_at", now); put("pinned", 0)
        }
        val id = db.insertWithOnConflict("clips", null, v, SQLiteDatabase.CONFLICT_REPLACE)
        if (id > 0) prune(db)
        return id > 0
    }

    fun togglePin(id: Long) {
        val db = writableDatabase
        db.execSQL("UPDATE clips SET pinned = CASE WHEN pinned=1 THEN 0 ELSE 1 END WHERE id=?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("clips", "id=?", arrayOf(id.toString()))
    }

    fun clearAll() {
        writableDatabase.delete("clips", null, null)
    }

    fun purgeOlderThan(days: Int, now: Long = System.currentTimeMillis()) {
        val cutoff = now - days * 24L * 60 * 60 * 1000
        writableDatabase.delete("clips", "pinned=0 AND created_at<?", arrayOf(cutoff.toString()))
    }

    fun clipboardFromSystemText(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return add(text)
    }

    private fun prune(db: SQLiteDatabase) {
        db.execSQL("DELETE FROM clips WHERE pinned=0 AND id NOT IN (SELECT id FROM clips WHERE pinned=0 ORDER BY created_at DESC LIMIT 50)")
        db.execSQL("DELETE FROM clips WHERE pinned=1 AND id NOT IN (SELECT id FROM clips WHERE pinned=1 ORDER BY created_at DESC LIMIT 10)")
    }

    companion object {
        fun sha256(s: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        }
    }
}
