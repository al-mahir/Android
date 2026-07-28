package com.iti.data.sheikh.local

import com.iti.domain.model.SheikhAvailability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AlmahirSheikhLocalDataSource {

    fun observeAvailability(): Flow<SheikhAvailability>

    suspend fun setAvailability(availability: SheikhAvailability)
}

/**
 * In-memory store — there's no write endpoint for availability yet. Swapping this for a
 * persisted (or fully server-backed) implementation later is a one-line Koin binding change,
 * same convention as [com.iti.data.datasource.AlmahirFakeDataSource].
 */
class InMemoryAlmahirSheikhLocalDataSource : AlmahirSheikhLocalDataSource {

    private val availability = MutableStateFlow(SheikhAvailability.OFFLINE)

    override fun observeAvailability(): Flow<SheikhAvailability> = availability.asStateFlow()

    override suspend fun setAvailability(availability: SheikhAvailability) {
        this.availability.value = availability
    }
}
