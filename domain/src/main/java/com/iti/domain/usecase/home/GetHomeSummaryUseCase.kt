package com.iti.domain.usecase.home

import com.iti.domain.model.home.HomeSummary
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow

/** Streams the Home screen's content. */
class GetHomeSummaryUseCase(
    private val repository: AlmahirRepository,
) {
    operator fun invoke(): Flow<HomeSummary> = repository.observeHomeSummary()
}
