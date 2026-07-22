package com.iti.domain.usecase

import com.iti.domain.model.recitation.RecitationSessionSummary
import com.iti.domain.repository.RecitationSessionRepository
import kotlinx.coroutines.flow.Flow

/** The reciter's history, newest first. */
class ObserveRecitationSessionsUseCase(
    private val repository: RecitationSessionRepository,
) {
    operator fun invoke(): Flow<List<RecitationSessionSummary>> = repository.observeSessions()
}

/** One session's detail, for review long after it was recited. */
class GetRecitationSessionUseCase(
    private val repository: RecitationSessionRepository,
) {
    operator fun invoke(id: String): Flow<RecitationSessionSummary?> = repository.observeSession(id)
}

/** Records a finished session so the reciter can come back to it. */
class SaveRecitationSessionUseCase(
    private val repository: RecitationSessionRepository,
) {
    suspend operator fun invoke(summary: RecitationSessionSummary) = repository.save(summary)
}

/** Removes one session. History is the reciter's own record, so they may discard it. */
class DeleteRecitationSessionUseCase(
    private val repository: RecitationSessionRepository,
) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}
