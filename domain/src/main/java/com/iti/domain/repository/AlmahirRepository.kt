package com.iti.domain.repository

import com.iti.domain.model.home.HomeSummary
import kotlinx.coroutines.flow.Flow

/**
 * Aggregate repository for the app's general (non-Mushaf) content. Implemented in `:data`;
 * `domain` owns only this contract, so the fake data source in use today can be swapped for
 * the real backend without touching a use case or a ViewModel.
 */
interface AlmahirRepository {

    /**
     * Home content as a stream, so a later join (or a background refresh) re-emits and the
     * screen updates without an explicit reload.
     */
    fun observeHomeSummary(): Flow<HomeSummary>

    /** Joins the circle identified by [circleId]. Throws on failure. */
    suspend fun joinCircle(circleId: String)
}
