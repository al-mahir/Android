package com.iti.data.repository

import com.iti.data.datasource.home.HomeDataSource
import com.iti.data.mapper.home.toDomain
import com.iti.domain.model.home.HomeSummary
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Binds [AlmahirRepository] to a [HomeDataSource]. Deliberately thin: it maps DTO -> domain
 * and nothing else, so replacing [com.iti.data.datasource.home.FakeHomeDataSource] with the
 * real remote source requires no change here.
 */
class AlmahirRepositoryImpl(
    private val homeDataSource: HomeDataSource,
) : AlmahirRepository {

    override fun observeHomeSummary(): Flow<HomeSummary> =
        homeDataSource.observeHomeSummary().map { dto -> dto.toDomain() }

    override suspend fun joinCircle(circleId: String) {
        homeDataSource.joinCircle(circleId)
    }
}
