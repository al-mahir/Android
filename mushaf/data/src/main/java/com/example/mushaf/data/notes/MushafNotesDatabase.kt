package com.example.mushaf.data.notes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Mushaf's user-writable database (per-ayah personal notes).
 *
 * Kept separate from the read-only bundled layout assets and from the app's
 * [com.iti.data.local.AlmahirDatabase] so notes survive mushaf data updates.
 */
@Database(
    entities = [AyahNoteEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MushafNotesDatabase : RoomDatabase() {

    abstract fun ayahNoteDao(): AyahNoteDao

    companion object {
        private const val NAME = "mushaf_notes.db"

        fun create(context: Context): MushafNotesDatabase =
            Room.databaseBuilder(context.applicationContext, MushafNotesDatabase::class.java, NAME)
                .build()
    }
}
