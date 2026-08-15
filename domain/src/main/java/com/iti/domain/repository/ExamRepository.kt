package com.iti.domain.repository

import com.iti.domain.model.exam.ExamSummary
import com.iti.domain.model.exam.RecentExamScope
import com.iti.domain.model.exam.ExamScope
import kotlinx.coroutines.flow.Flow

/**
 * Persists exam results and exposes recent exam scope history.
 *
 * Implementations live in `:data` (Room-backed); the interface is owned by `:domain`
 * so use cases never depend on Room.
 */
interface ExamRepository {

    /** Persist a completed (or partial) exam summary. */
    suspend fun save(summary: ExamSummary)

    /** Observe the most recent exam scopes (newest first, capped at 5 entries). */
    fun observeRecentScopes(): Flow<List<RecentExamScope>>
    
    suspend fun saveCustomScope(scope: ExamScope.CustomRange)
    suspend fun getCustomScopes(): List<ExamScope.CustomRange>
    suspend fun deleteCustomScope(id: String)

    /** Retrieve a specific exam summary by its ID. */
    suspend fun getById(id: String): ExamSummary?
}
