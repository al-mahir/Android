package com.iti.data.sheikh.repository

import com.iti.data.core.token.TokenStore
import com.iti.data.mapper.toDomain
import com.iti.data.sheikh.local.AlmahirSheikhLocalDataSource
import com.iti.data.sheikh.remote.AlmahirSheikhRemoteDataSource
import com.iti.domain.core.Result
import com.iti.domain.core.asResult
import com.iti.domain.core.resultOf
import com.iti.domain.model.SheikhAvailability
import com.iti.domain.repository.AlmahirSheikhRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * "All other sheikh features" repository (availability today; circle-hosting and session
 * requests extend this same class as their endpoints land) — the sheikh-side counterpart to
 * [com.iti.data.repository.AlmahirRepositoryImpl].
 */
class AlmahirSheikhRepositoryImpl(
    private val local: AlmahirSheikhLocalDataSource,
    private val remote: AlmahirSheikhRemoteDataSource,
    private val tokenStore: TokenStore,
) : AlmahirSheikhRepository {

    private val seedMutex = Mutex()
    private var seeded = false

    override fun observeMyAvailability(): Flow<Result<SheikhAvailability>> =
        local.observeAvailability()
            .onStart { seedFromRemoteOnce() }
            .asResult()

    override suspend fun setMyAvailability(availability: SheikhAvailability): Result<Unit> =
        resultOf { local.setAvailability(availability) }

    /** Best-effort: seeds the local state from the server's real status once. No write endpoint exists yet, so failures just leave the local default. */
    private suspend fun seedFromRemoteOnce() {
        seedMutex.withLock {
            if (seeded) return
            seeded = true
            val id = tokenStore.getUserId() ?: return
            runCatching { remote.getMyProfile(id) }
                .getOrNull()
                ?.toDomain()
                ?.let { sheikh -> local.setAvailability(sheikh.availability) }
        }
    }
}
