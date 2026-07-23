package com.iti.domain.repository

import com.iti.domain.model.recitation.RecitationSessionSummary
import kotlinx.coroutines.flow.Flow


interface RecitationSessionRepository {

    fun observeSessions(): Flow<List<RecitationSessionSummary>>

    fun observeSession(id: String): Flow<RecitationSessionSummary?>


    suspend fun save(summary: RecitationSessionSummary)

    suspend fun delete(id: String)

    suspend fun deleteAll()
}
