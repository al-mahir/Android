package com.iti.domain.usecase.circle

import com.iti.domain.model.StudyCircle
import com.iti.domain.repository.CircleRepository
import kotlinx.coroutines.flow.Flow

class GetStudyCirclesUseCase(
    private val repository: CircleRepository,
) {
    operator fun invoke(): Flow<List<StudyCircle>> =
        repository.observeStudyCircles()
}
