package com.iti.domain.repository

import com.iti.domain.core.Result
import com.iti.domain.model.recitation.RecitationSessionSummary
import kotlinx.coroutines.flow.Flow


interface RecitationSessionRepository {

    fun observeSessions(): Flow<Result<List<RecitationSessionSummary>>>

    fun observeSession(id: String): Flow<Result<RecitationSessionSummary?>>

    suspend fun save(summary: RecitationSessionSummary): Result<Unit>

    suspend fun delete(id: String): Result<Unit>

    suspend fun deleteAll(): Result<Unit>
}
