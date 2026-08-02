package com.iti.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iti.data.local.recitation.RecitationSessionDao
import com.iti.data.local.recitation.RecitationSessionEntity


@Database(
    entities = [RecitationSessionEntity::class, com.iti.data.local.bookmark.BookmarkEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AlmahirDatabase : RoomDatabase() {

    abstract fun recitationSessionDao(): RecitationSessionDao

    abstract fun bookmarkDao(): com.iti.data.local.bookmark.BookmarkDao

    companion object {
        private const val NAME = "almahir.db"

        fun create(context: Context): AlmahirDatabase =
            Room.databaseBuilder(context.applicationContext, AlmahirDatabase::class.java, NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
