package com.example.mushaf.data.recitation.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecitationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahTimings(timings: List<AyahTimingEntity>)

    @Query("SELECT * FROM ayah_timings WHERE reciterId = :reciterId AND verseKey IN (:verseKeys)")
    suspend fun getAyahTimings(reciterId: Int, verseKeys: List<String>): List<AyahTimingEntity>

    @Query("SELECT * FROM ayah_timings WHERE reciterId = :reciterId")
    suspend fun getAllTimingsForReciter(reciterId: Int): List<AyahTimingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDownloadStatus(status: DownloadStatusEntity)

    @Query("SELECT * FROM download_status WHERE id = :id")
    suspend fun getDownloadStatus(id: String): DownloadStatusEntity?

    @Query("SELECT * FROM download_status WHERE id = :id")
    fun observeDownloadStatus(id: String): Flow<DownloadStatusEntity?>

    @Query("SELECT * FROM download_status WHERE reciterId = :reciterId")
    fun observeAllDownloadStatusesForReciter(reciterId: Int): Flow<List<DownloadStatusEntity>>

    @Query("DELETE FROM ayah_timings WHERE reciterId = :reciterId AND verseKey LIKE :surahId || ':%'")
    suspend fun deleteTimingsForSurah(reciterId: Int, surahId: Int)

    @Query("DELETE FROM ayah_timings WHERE reciterId = :reciterId")
    suspend fun deleteAllTimingsForReciter(reciterId: Int)
}
