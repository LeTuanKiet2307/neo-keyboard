package app.neokeyboard.suggest

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** SQLite từ người dùng (SPEC §5.7): words + bigrams, ghi theo lô, giới hạn 10k/50k. */
class UserLexiconStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext, "user_lexicon.db", null, 1,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE words (word TEXT PRIMARY KEY, freq INTEGER NOT NULL, last_used INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE bigrams (prev TEXT NOT NULL, next TEXT NOT NULL, count INTEGER NOT NULL, last_used INTEGER NOT NULL, PRIMARY KEY(prev, next))")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) = Unit

    fun wordFreq(word: String): Int {
        val db = readableDatabase
        db.rawQuery("SELECT freq FROM words WHERE word=?", arrayOf(word)).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun nextWords(prev: String, limit: Int): List<Pair<String, Int>> {
        val db = readableDatabase
        db.rawQuery("SELECT next, count FROM bigrams WHERE prev=? ORDER BY count DESC LIMIT ?", arrayOf(prev, limit.toString())).use { c ->
            val out = mutableListOf<Pair<String, Int>>()
            while (c.moveToNext()) out.add(c.getString(0) to c.getInt(1))
            return out
        }
    }

    fun learnWord(word: String, now: Long) {
        if (word.isEmpty() || word.length > 30) return
        val db = writableDatabase
        val cur = wordFreq(word)
        val v = ContentValues().apply {
            put("word", word); put("freq", cur + 1); put("last_used", now)
        }
        db.replace("words", null, v)
        prune(db)
    }

    fun learnBigram(prev: String, next: String, now: Long) {
        if (prev.isEmpty() || next.isEmpty()) return
        val db = writableDatabase
        var cnt = 0
        db.rawQuery("SELECT count FROM bigrams WHERE prev=? AND next=?", arrayOf(prev, next)).use { c ->
            if (c.moveToFirst()) cnt = c.getInt(0)
        }
        val v = ContentValues().apply {
            put("prev", prev); put("next", next); put("count", cnt + 1); put("last_used", now)
        }
        db.replace("bigrams", null, v)
        prune(db)
    }

    fun clearAll() {
        val db = writableDatabase
        db.delete("words", null, null)
        db.delete("bigrams", null, null)
    }

    private fun prune(db: SQLiteDatabase) {
        db.execSQL("DELETE FROM words WHERE word NOT IN (SELECT word FROM words ORDER BY last_used DESC LIMIT 10000)")
        db.execSQL("DELETE FROM bigrams WHERE (prev, next) NOT IN (SELECT prev, next FROM bigrams ORDER BY last_used DESC LIMIT 50000)")
    }
}
