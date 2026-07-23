package com.example.mushaf.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.TafsirResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TafsirDataSource(
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
        Log.d(MushafLog.TAG, "Opening Tafsir DB at ${target.absolutePath} (exists=${target.exists()}, stale=$stale)")
        if (!target.exists() || target.length() == 0L || stale) {
            copyAssetTo(target)
            markVersion(target)
        }
        return SQLiteDatabase.openDatabase(
            target.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).also {
            Log.d(MushafLog.TAG, "Tafsir DB opened (version=${it.version})")
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
            .onFailure { Log.w(MushafLog.TAG, "Failed to write Tafsir DB version marker", it) }
    }

    private fun copyAssetTo(target: File) {
        Log.d(MushafLog.TAG, "Copying DB asset '$ASSET_PATH' -> ${target.absolutePath}")
        target.parentFile?.mkdirs()
        try {
            context.assets.open(ASSET_PATH).use { input ->
                FileOutputStream(target).use { output ->
                    val bytes = input.copyTo(output)
                    Log.d(MushafLog.TAG, "Copied $bytes bytes for Tafsir DB asset")
                }
            }
        } catch (t: Throwable) {
            Log.e(MushafLog.TAG, "Failed to copy Tafsir DB asset '$ASSET_PATH'", t)
            runCatching { if (target.exists()) target.delete() }
            throw t
        }
    }

    suspend fun getTafsirForAyah(surah: Int, ayah: Int): TafsirResult? = withContext(Dispatchers.IO) {
        val ayahKey = "$surah:$ayah"
        var tafsirText: String? = null

        try {
            val sql = "SELECT text, group_ayah_key FROM tafsir WHERE ayah_key = ?"
            var groupAyahKey: String? = null
            
            database().rawQuery(sql, arrayOf(ayahKey)).use { c ->
                if (c.moveToFirst()) {
                    tafsirText = c.getString(0)
                    groupAyahKey = if (!c.isNull(1)) c.getString(1) else null
                }
            }

            if (groupAyahKey != null) {
                // Fetch the group's tafsir instead
                val groupSql = "SELECT text FROM tafsir WHERE ayah_key = ?"
                database().rawQuery(groupSql, arrayOf(groupAyahKey)).use { c ->
                    if (c.moveToFirst()) {
                        tafsirText = c.getString(0)
                    }
                }
            }

            return@withContext tafsirText?.let {
                TafsirResult(
                    surahNumber = surah,
                    ayahNumber = ayah,
                    tafsirText = it
                )
            }
        } catch (e: Exception) {
            Log.e(MushafLog.TAG, "Error fetching Tafsir for $ayahKey", e)
        }
        return@withContext null
    }

    suspend fun searchTafsir(query: String, limit: Int = 50, offset: Int = 0): List<TafsirResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TafsirResult>()
        val q = query.trim()

        if (q.isBlank()) return@withContext results

        try {
            val sql = "SELECT ayah_key, text FROM tafsir WHERE text LIKE ? ORDER BY ayah_key LIMIT ? OFFSET ?"
            val args = arrayOf("%$q%", limit.toString(), offset.toString())

            database().rawQuery(sql, args).use { c ->
                while (c.moveToNext()) {
                    val key = c.getString(0)
                    val parts = key.split(":")
                    if (parts.size == 2) {
                        val surah = parts[0].toIntOrNull() ?: continue
                        val ayah = parts[1].toIntOrNull() ?: continue
                        results.add(
                            TafsirResult(
                                surahNumber = surah,
                                ayahNumber = ayah,
                                tafsirText = c.getString(1)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(MushafLog.TAG, "Error searching Tafsir for query: $q", e)
        }
        return@withContext results
    }

    companion object {
        private const val DB_VERSION = 1
        private const val DB_NAME = "tafsir-mukhtasar.db"
        private const val ASSET_PATH = "databases/tafsir-mukhtasar.db"
    }
}
