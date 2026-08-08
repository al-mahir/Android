package com.iti.data.local.meeting

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingStatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meetingStatus: MeetingStatusEntity)

    @Query("SELECT * FROM meeting_status WHERE userId = :userId ORDER BY meetingTime DESC")
    fun observeMeetingStatuses(userId: String): Flow<List<MeetingStatusEntity>>

    @Query("DELETE FROM meeting_status WHERE id = :id")
    suspend fun delete(id: String)
}
