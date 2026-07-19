package com.iti.domain.usecase.circle

import com.iti.domain.model.StudyCircle
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class GetStudyCirclesUseCase(
    private val repository: AlmahirRepository,
) {
    operator fun invoke(): Flow<List<StudyCircle>> =
        repository.observeStudyCircles().map { circles ->
            circles.sortedBy { circle -> circle.isJoined }
        }
}
