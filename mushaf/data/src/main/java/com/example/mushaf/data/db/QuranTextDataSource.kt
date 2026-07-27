package com.example.mushaf.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.mushaf.data.MushafLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class RawAyahResult(
    val surahNumber: Int,
    val ayahNumber: Int,
    val text: String
)

class QuranTextDataSource(
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
        Log.d(MushafLog.TAG, "Opening Quran Text DB at ${target.absolutePath} (exists=${target.exists()}, stale=$stale)")
        if (!target.exists() || target.length() == 0L || stale) {
            copyAssetTo(target)
            markVersion(target)
        }
        return SQLiteDatabase.openDatabase(
            target.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).also {
            Log.d(MushafLog.TAG, "Quran Text DB opened (version=${it.version})")
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
            .onFailure { Log.w(MushafLog.TAG, "Failed to write Quran Text DB version marker", it) }
    }

    private fun copyAssetTo(target: File) {
        Log.d(MushafLog.TAG, "Copying DB asset '$ASSET_PATH' -> ${target.absolutePath}")
        target.parentFile?.mkdirs()
        try {
            context.assets.open(ASSET_PATH).use { input ->
                FileOutputStream(target).use { output ->
                    val bytes = input.copyTo(output)
                    Log.d(MushafLog.TAG, "Copied $bytes bytes for Quran Text DB asset")
                }
            }
        } catch (t: Throwable) {
            Log.e(MushafLog.TAG, "Failed to copy Quran Text DB asset '$ASSET_PATH'", t)
            runCatching { if (target.exists()) target.delete() }
            throw t
        }
    }

    suspend fun searchAyahs(query: String, limit: Int = 50, offset: Int = 0): List<RawAyahResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RawAyahResult>()
        val q = query.trim()

        try {
            val sql: String
            val args: Array<String>

            if (q.isBlank()) {
                sql = "SELECT surah, ayah, text FROM verses LIMIT ? OFFSET ?"
                args = arrayOf(limit.toString(), offset.toString())
            } else {
                sql = "SELECT surah, ayah, text FROM verses WHERE text LIKE ? LIMIT ? OFFSET ?"
                args = arrayOf("%$q%", limit.toString(), offset.toString())
            }

            database().rawQuery(sql, args).use { c ->
                while (c.moveToNext()) {
                    results.add(
                        RawAyahResult(
                            surahNumber = c.getInt(0),
                            ayahNumber = c.getInt(1),
                            text = c.getString(2)
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            Log.e(MushafLog.TAG, "Ayah search failed for query: $query", t)
            throw t
        }
        results
    }

       suspend fun getVerseText(sura: Int, ayah: Int): String? = withContext(Dispatchers.IO) {
        try {
            database().rawQuery(QUERY_VERSE_TEXT, arrayOf(sura.toString(), ayah.toString())).use { c ->
                if (c.moveToFirst()) return@withContext c.getString(0)
            }
        } catch (t: Throwable) {
            Log.e(MushafLog.TAG, "Verse text query failed for $sura:$ayah", t)
            throw t
        }
        null
    }

    private companion object {
        const val DB_NAME = "quran_text.db"
        const val ASSET_PATH = "databases/quran_text.db"
        const val DB_VERSION = 1
        const val QUERY_VERSE_TEXT = "SELECT text FROM verses WHERE surah = ? AND ayah = ? LIMIT 1"
    }
}
