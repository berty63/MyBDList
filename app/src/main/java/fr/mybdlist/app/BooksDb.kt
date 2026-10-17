package fr.mybdlist.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Album(
    val id: Long = 0,
    val title: String,
    val series: String = "",
    val volume: String = "",
    val author: String = "",
    val isbn: String = "",
    val owned: Boolean = true,
    val notes: String = ""
)

class BooksDb(context: Context) : SQLiteOpenHelper(context, "mybdlist.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE albums (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, series TEXT NOT NULL DEFAULT '', volume TEXT NOT NULL DEFAULT '', author TEXT NOT NULL DEFAULT '', isbn TEXT NOT NULL DEFAULT '', owned INTEGER NOT NULL DEFAULT 1, notes TEXT NOT NULL DEFAULT '')""")
        db.execSQL("CREATE INDEX idx_album_series ON albums(series)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun insert(a: Album): Long {
        val values = ContentValues().apply {
            put("title", a.title.trim()); put("series", a.series.trim()); put("volume", a.volume.trim())
            put("author", a.author.trim()); put("isbn", a.isbn.trim()); put("owned", if (a.owned) 1 else 0); put("notes", a.notes.trim())
        }
        return writableDatabase.insertOrThrow("albums", null, values)
    }
    fun update(a: Album) {
        val values = ContentValues().apply {
            put("title", a.title.trim()); put("series", a.series.trim()); put("volume", a.volume.trim())
            put("author", a.author.trim()); put("isbn", a.isbn.trim()); put("owned", if (a.owned) 1 else 0); put("notes", a.notes.trim())
        }
        writableDatabase.update("albums", values, "id = ?", arrayOf(a.id.toString()))
    }
    fun delete(id: Long) { writableDatabase.delete("albums", "id = ?", arrayOf(id.toString())) }
    fun all(query: String = ""): List<Album> {
        val result = mutableListOf<Album>()
        val selection = if (query.isBlank()) null else "title LIKE ? OR series LIKE ? OR author LIKE ? OR isbn LIKE ?"
        val args = if (selection == null) null else Array(4) { "%$query%" }
        readableDatabase.query("albums", null, selection, args, null, null, "series COLLATE NOCASE, volume COLLATE NOCASE, title COLLATE NOCASE").use { c ->
            while (c.moveToNext()) {
                result += Album(c.getLong(c.getColumnIndexOrThrow("id")), c.getString(c.getColumnIndexOrThrow("title")), c.getString(c.getColumnIndexOrThrow("series")), c.getString(c.getColumnIndexOrThrow("volume")), c.getString(c.getColumnIndexOrThrow("author")), c.getString(c.getColumnIndexOrThrow("isbn")), c.getInt(c.getColumnIndexOrThrow("owned")) == 1, c.getString(c.getColumnIndexOrThrow("notes")))
            }
        }
        return result
    }
    fun duplicate(a: Album): Boolean = all().any {
        if (a.isbn.isNotBlank() && it.isbn.isNotBlank()) it.isbn.equals(a.isbn, true)
        else a.series.isNotBlank() && a.volume.isNotBlank() && it.series.equals(a.series, true) && it.volume.equals(a.volume, true)
    }
}
