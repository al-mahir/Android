package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.ReaderPreferences
import kotlinx.coroutines.flow.Flow


interface ReaderPreferencesRepository {

    val preferences: Flow<ReaderPreferences>

    suspend fun setTajweedEnabled(enabled: Boolean)

    suspend fun setLastPage(page: Int)
}
