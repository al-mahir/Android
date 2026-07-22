package com.iti.data.local.recitation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecitationSessionDao {

    @Query("SELECT * FROM recitation_sessions ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<RecitationSessionEntity>>

    @Query("SELECT * FROM recitation_sessions WHERE id = :id")
    fun observeById(id: String): Flow<RecitationSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: RecitationSessionEntity)

    @Query("DELETE FROM recitation_sessions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM recitation_sessions")
    suspend fun deleteAll()
}
