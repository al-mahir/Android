package com.iti.domain.repository

import com.iti.domain.core.Result
import com.iti.domain.model.SheikhAvailability
import kotlinx.coroutines.flow.Flow

/**
 * The signed-in sheikh's own capabilities — availability now, circle-hosting and session
 * requests as they're added. Distinct from [SheikhRepository], which is the read-only
 * browse-other-sheikhs resource shared with the student app.
 */
interface AlmahirSheikhRepository {

    /** Observe the signed-in sheikh's own availability for 1:1 session requests. */
    fun observeMyAvailability(): Flow<Result<SheikhAvailability>>

    /** Update the signed-in sheikh's own availability. */
    suspend fun setMyAvailability(availability: SheikhAvailability): Result<Unit>
}
