package com.example.ritmo

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

data class PendingPost(
    val id: Long,
    val roomId: String,
    val authorId: String,
    val authorName: String,
    val category: String,
    val content: String,
    val isBoosted: Boolean,
    val imageBase64: String?,
    val createdAtMillis: Long
)

/**
 * Local SQLite queue for posts written while offline.
 * Reference: Android Developers (2024) Save data using SQLite.
 * https://developer.android.com/training/data-storage/sqlite
 */
class OfflineDb(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "ritmo_offline.db", null, 1) {

    private val tag = "RitmoOfflineDb"

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE pending_posts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                roomId TEXT NOT NULL,
                authorId TEXT NOT NULL,
                authorName TEXT NOT NULL,
                category TEXT NOT NULL,
                content TEXT NOT NULL,
                isBoosted INTEGER NOT NULL,
                imageBase64 TEXT,
                createdAtMillis INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS pending_posts")
        onCreate(db)
    }

    fun insert(
        roomId: String, authorId: String, authorName: String, category: String,
        content: String, isBoosted: Boolean, imageBase64: String?, createdAtMillis: Long
    ) {
        val values = ContentValues().apply {
            put("roomId", roomId)
            put("authorId", authorId)
            put("authorName", authorName)
            put("category", category)
            put("content", content)
            put("isBoosted", if (isBoosted) 1 else 0)
            put("imageBase64", imageBase64)
            put("createdAtMillis", createdAtMillis)
        }
        writableDatabase.insert("pending_posts", null, values)
        Log.d(tag, "Queued offline post for room $roomId")
    }

    fun getAll(): List<PendingPost> {
        val list = mutableListOf<PendingPost>()
        readableDatabase.rawQuery(
            "SELECT id, roomId, authorId, authorName, category, content, isBoosted, imageBase64, createdAtMillis " +
                    "FROM pending_posts ORDER BY createdAtMillis ASC", null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    PendingPost(
                        id = c.getLong(0),
                        roomId = c.getString(1),
                        authorId = c.getString(2),
                        authorName = c.getString(3),
                        category = c.getString(4),
                        content = c.getString(5),
                        isBoosted = c.getInt(6) == 1,
                        imageBase64 = c.getString(7),
                        createdAtMillis = c.getLong(8)
                    )
                )
            }
        }
        return list
    }

    fun countForRoom(roomId: String): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM pending_posts WHERE roomId = ?", arrayOf(roomId)
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun delete(id: Long) {
        writableDatabase.delete("pending_posts", "id = ?", arrayOf(id.toString()))
        Log.d(tag, "Removed synced post $id from local queue")
    }
}