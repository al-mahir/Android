package com.example.mushaf.data.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.mushaf.data.MushafLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MushafAssetDataSource(
    private val context: Context,
) {
    @Volatile
    private var db: SQLiteDatabase? = null

    private fun database(): SQLiteDatabase {
        db?.let { return it }
        return synchronized(this) {
            db ?: openDatabase().also { db = it }
        }
    }

    private fun openDatabase(): SQLiteDatabase {
        val target = context.getDatabasePath(DB_NAME)
        val stale = isStale(target)
        Log.d(MushafLog.TAG, "Opening Mushaf DB at ${target.absolutePath} (exists=${target.exists()}, stale=$stale)")
        if (!target.exists() || target.length() == 0L || stale) {
            copyAssetTo(target)
            markVersion(target)
        }
        return SQLiteDatabase.openDatabase(
            target.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).also {
            Log.d(MushafLog.TAG, "Mushaf DB opened (version=${it.version})")
        }
    }

    private fun isStale(target: File): Boolean {
        if (!target.exists()) return false
        val marker = versionFile()
        val current = runCatching { marker.readText().trim().toInt() }.getOrNull()
        return current != DB_VERSION
    }

    private fun versionFile(): File = File(context.getDatabasePath(DB_NAME).parentFile, "$DB_NAME.version")

    private fun markVersion(target: File) {
        runCatching { versionFile().writeText(DB_VERSION.toString()) }
            .onFailure { Log.w(MushafLog.TAG, "Failed to write Mushaf DB version marker", it) }
    }

    private fun copyAssetTo(target: File) {
        Log.d(MushafLog.TAG, "Copying DB asset '$ASSET_PATH' -> ${target.absolutePath}")
        target.parentFile?.mkdirs()
        try {
            context.assets.open(ASSET_PATH).use { input ->
                FileOutputStream(target).use { output ->
                    val bytes = input.copyTo(output)
                    Log.d(MushafLog.TAG, "Copied $bytes bytes for Mushaf DB asset")
                }
            }
        } catch (t: Throwable) {
            Log.e(MushafLog.TAG, "Failed to copy Mushaf DB asset '$ASSET_PATH'", t)
            runCatching { if (target.exists()) target.delete() }
            throw t
        }
    }

    suspend fun getLinesForPage(page: Int): List<MushafLineEntity> = withContext(Dispatchers.IO) {
        val rows = ArrayList<MushafLineEntity>(LINES_PER_PAGE_HINT)
        try {
            database().rawQuery(QUERY_LINES_FOR_PAGE, arrayOf(page.toString())).use { c ->
                while (c.moveToNext()) {
                    rows += c.toLineEntity()
                }
            }
        } catch (t: Throwable) {
            Log.e(MushafLog.TAG, "Line query failed for page $page", t)
            throw t
        }
        if (rows.isEmpty()) {
            Log.w(MushafLog.TAG, "No lines found for page $page (out of range or empty asset)")
        }
        rows
    }

    suspend fun getWordsForPage(page: Int): List<MushafWordEntity> = withContext(Dispatchers.IO) {
        val rows = ArrayList<MushafWordEntity>(LINES_PER_PAGE_HINT * 8)
        try {
            database().rawQuery(QUERY_WORDS_FOR_PAGE, arrayOf(page.toString())).use { c ->
                while (c.moveToNext()) {
                    rows += c.toWordEntity()
                }
            }
        } catch (t: Throwable) {
            Log.e(MushafLog.TAG, "Word query failed for page $page", t)
            throw t
        }
        Log.d(MushafLog.TAG, "Loaded ${rows.size} words for page $page")
        rows
    }

    suspend fun getPageCount(): Int = withContext(Dispatchers.IO) {
        try {
            database().rawQuery(QUERY_PAGE_COUNT, null).use { c ->
                if (c.moveToFirst() && !c.isNull(0)) {
                    val count = c.getInt(0)
                    Log.d(MushafLog.TAG, "Page count from info table = $count")
                    return@withContext count
                }
            }
        } catch (t: Throwable) {
            Log.e(MushafLog.TAG, "Failed to read page count; falling back to $DEFAULT_PAGE_COUNT", t)
        }
        DEFAULT_PAGE_COUNT
    }

    suspend fun searchPage(query: String): List<Int> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()

        val results = mutableListOf<Int>()
        try {
            val sql = "SELECT DISTINCT page_number FROM pages WHERE CAST(page_number AS TEXT) LIKE ? ORDER BY page_number ASC LIMIT 20"
            database().rawQuery(sql, arrayOf("$q%")).use { c ->
                while (c.moveToNext()) {
                    results.add(c.getInt(0))
                }
            }
        } catch (t: Throwable) {
            Log.e(MushafLog.TAG, "Page search failed for query: $query", t)
        }
        results
    }

    private fun Cursor.toLineEntity(): MushafLineEntity = MushafLineEntity(
        pageNumber = getIntByName("page_number") ?: 0,
        lineNumber = getIntByName("line_number") ?: 0,
        lineType = getStringByName("line_type").orEmpty(),
        isCentered = getIntByName("is_centered") ?: 0,
        surahNumber = getIntByName("surah_number"),
    )

    private fun Cursor.toWordEntity(): MushafWordEntity = MushafWordEntity(
        pageNumber = getIntByName("page_number") ?: 0,
        lineNumber = getIntByName("line_number") ?: 0,
        position = getIntByName("position") ?: 0,
        wordKey = getStringByName("word_key").orEmpty(),
        charType = getStringByName("char_type").orEmpty(),
        glyphText = getStringByName("glyph_text").orEmpty(),
    )

    private fun Cursor.getIntByName(name: String): Int? {
        val index = getColumnIndex(name)
        if (index < 0 || isNull(index)) return null
        val raw = getString(index)
        if (raw.isNullOrBlank()) return null
        return raw.trim().toIntOrNull()
    }

    private fun Cursor.getStringByName(name: String): String? {
        val index = getColumnIndex(name)
        if (index < 0 || isNull(index)) return null
        return getString(index)
    }

    private companion object {
        const val DB_NAME = "mushaf_v4_layout.db"
        const val ASSET_PATH = "databases/mushaf_v4_layout.db"

        const val DB_VERSION = 2

        const val DEFAULT_PAGE_COUNT = 604
        const val LINES_PER_PAGE_HINT = 15

        const val QUERY_LINES_FOR_PAGE =
            "SELECT page_number, line_number, line_type, is_centered, surah_number " +
                "FROM pages WHERE page_number = ? ORDER BY line_number ASC"
        const val QUERY_WORDS_FOR_PAGE =
            "SELECT page_number, line_number, position, word_key, char_type, glyph_text " +
                "FROM words WHERE page_number = ? ORDER BY line_number ASC, position ASC"
        const val QUERY_PAGE_COUNT = "SELECT number_of_pages FROM info LIMIT 1"
    }
}
