package com.iti.data.datasource.circle

import com.iti.data.dto.StudyCircleDto
import kotlinx.coroutines.flow.Flow

interface CircleDataSource {

    fun observeStudyCircles(): Flow<List<StudyCircleDto>>

    suspend fun joinStudyCircle(circleId: String)

    suspend fun cancelJoinCircle(circleId: String)
}
