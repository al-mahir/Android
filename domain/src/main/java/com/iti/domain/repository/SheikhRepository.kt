package com.iti.domain.repository

import com.iti.domain.core.Result
import com.iti.domain.model.Sheikh

interface SheikhRepository {

    /** Fetch all sheikhs from the remote source. */
    suspend fun getSheikhs(): Result<List<Sheikh>>

    /** Fetch a single sheikh by their UUID. Success with null data means not found. */
    suspend fun getSheikhById(id: String): Result<Sheikh?>

    /** Search sheikhs by name prefix. Returns matching sheikhs. */
    suspend fun searchSheikhs(name: String): Result<List<Sheikh>>
}
