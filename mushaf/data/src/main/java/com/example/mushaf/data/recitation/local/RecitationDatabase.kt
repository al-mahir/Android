package com.example.mushaf.data.recitation.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AyahTimingEntity::class, DownloadStatusEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RecitationDatabase : RoomDatabase() {
    abstract fun recitationDao(): RecitationDao
}
