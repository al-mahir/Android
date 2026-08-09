package com.iti.data.local.exam

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Entity(tableName = "exam_summaries")
data class ExamSummaryEntity(
    @PrimaryKey val id: String,
    val scopeJson: String,
    val startedAtMs: Long,
    val totalDurationMs: Long,
    val totalQuestions: Int,
    val correctCount: Int,
    val mistakeCount: Int,
    val skippedCount: Int,
    val accuracy: Float,
    val questionResultsJson: String,
)

// ─── DAO ─────────────────────────────────────────────────────────────────────

@Dao
interface ExamSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExamSummaryEntity)

    @Query("SELECT * FROM exam_summaries ORDER BY startedAtMs DESC")
    fun observeAll(): Flow<List<ExamSummaryEntity>>

    @Query("SELECT * FROM exam_summaries ORDER BY startedAtMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 5): Flow<List<ExamSummaryEntity>>

    @Query("SELECT * FROM exam_summaries WHERE id = :id")
    suspend fun getById(id: String): ExamSummaryEntity?

    @Query("DELETE FROM exam_summaries WHERE id = :id")
    suspend fun deleteById(id: String)
}
