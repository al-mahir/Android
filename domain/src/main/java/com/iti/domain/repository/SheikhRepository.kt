package com.iti.domain.repository

import com.iti.domain.model.Sheikh

interface SheikhRepository {

    /** Fetch all sheikhs from the remote source. */
    suspend fun getSheikhs(): List<Sheikh>

    /** Fetch a single sheikh by their UUID. Returns null if not found. */
    suspend fun getSheikhById(id: String): Sheikh?

    /** Search sheikhs by name prefix. Returns matching sheikhs. */
    suspend fun searchSheikhs(name: String): List<Sheikh>
}
