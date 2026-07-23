package com.iti.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iti.data.local.recitation.RecitationSessionDao
import com.iti.data.local.recitation.RecitationSessionEntity

/**
 * The app's own writable database, for data the reciter produces.
 *
 * Distinct from the Mushaf's bundled layout database, which is a read-only asset shipped with the
 * app. Mixing the two would put a user's session history in a file that is replaced wholesale on
 * every muṣḥaf data update.
 */
@Database(
    entities = [RecitationSessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AlmahirDatabase : RoomDatabase() {

    abstract fun recitationSessionDao(): RecitationSessionDao

    companion object {
        private const val NAME = "almahir.db"

        fun create(context: Context): AlmahirDatabase =
            Room.databaseBuilder(context.applicationContext, AlmahirDatabase::class.java, NAME)
                .build()
    }
}
