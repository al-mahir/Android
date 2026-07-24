package com.iti.data.repository

import com.iti.data.datasource.circle.CircleDataSource
import com.iti.data.mapper.toDomain
import com.iti.domain.model.StudyCircle
import com.iti.domain.repository.CircleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CircleRepositoryImpl(
    private val dataSource: CircleDataSource,
) : CircleRepository {

    override fun observeStudyCircles(): Flow<List<StudyCircle>> =
        dataSource.observeStudyCircles().map { dtos -> dtos.map { it.toDomain() } }

    override fun observeSheikhCircles(sheikhId: String): Flow<List<StudyCircle>> =
        dataSource.observeStudyCircles().map { dtos ->
            dtos.filter { it.hostId == sheikhId }.map { it.toDomain() }
        }

    override suspend fun joinStudyCircle(circleId: String) {
        dataSource.joinStudyCircle(circleId)
    }

    override suspend fun cancelJoinCircle(circleId: String) {
        dataSource.cancelJoinCircle(circleId)
    }
}
