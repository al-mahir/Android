package com.iti.domain.repository

import com.iti.domain.model.StudyCircle
import kotlinx.coroutines.flow.Flow

interface CircleRepository {

    /** Observe the list of study circles (supports real-time updates from fake/local source). */
    fun observeStudyCircles(): Flow<List<StudyCircle>>

    /** Observe circles for a specific sheikh. */
    fun observeSheikhCircles(sheikhId: String): Flow<List<StudyCircle>>

    /** Join a study circle. */
    suspend fun joinStudyCircle(circleId: String)

    /** Cancel a pending join request. */
    suspend fun cancelJoinCircle(circleId: String)
}
