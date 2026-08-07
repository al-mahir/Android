package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.ReaderPreferences
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow


interface ReaderPreferencesRepository {

    val preferences: Flow<ReaderPreferences>

    suspend fun setTajweedEnabled(enabled: Boolean): Result<Unit>

    suspend fun setLastPage(page: Int): Result<Unit>

    suspend fun setFirstMushafLaunchCompleted(): Result<Unit>
    
    suspend fun setDownloadOverWifiOnly(enabled: Boolean): Result<Unit>
}
