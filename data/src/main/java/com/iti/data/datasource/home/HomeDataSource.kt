package com.iti.data.datasource.home

import com.iti.data.dto.home.HomeSummaryDto
import kotlinx.coroutines.flow.Flow

/**
 * Source of Home content. The backend is not ready yet, so [FakeHomeDataSource] is bound in
 * DI today; swapping in a Ktor-backed implementation later is a one-line change in
 * `almahirDataModule` and touches nothing above `:data`.
 */
interface HomeDataSource {

    fun observeHomeSummary(): Flow<HomeSummaryDto>

    suspend fun joinCircle(circleId: String)
}
