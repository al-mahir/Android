package com.example.mushaf.data.repository

import com.example.mushaf.data.prefs.ReaderPreferencesDataStore
import com.example.mushaf.data.prefs.RecitationSettingsDataStore
import com.example.mushaf.domain.model.ReaderPreferences
import com.example.mushaf.domain.model.recite.RecitationSettings
import com.example.mushaf.domain.repository.ReaderPreferencesRepository
import com.example.mushaf.domain.repository.RecitationSettingsRepository
import com.iti.domain.core.Result
import com.iti.domain.core.resultOf
import kotlinx.coroutines.flow.Flow

class MushafPreferencesRepositoryImpl(
    private val readerDataStore: ReaderPreferencesDataStore,
    private val recitationSettingsDataStore: RecitationSettingsDataStore,
) : ReaderPreferencesRepository, RecitationSettingsRepository {

    // ── ReaderPreferencesRepository ──────────────────────────────────────

    override val preferences: Flow<ReaderPreferences> = readerDataStore.preferences

    override suspend fun setTajweedEnabled(enabled: Boolean): Result<Unit> =
        resultOf { readerDataStore.setTajweedEnabled(enabled) }

    override suspend fun setLastPage(page: Int): Result<Unit> =
        resultOf { readerDataStore.setLastPage(page) }

    override suspend fun setFirstMushafLaunchCompleted(): Result<Unit> =
        resultOf { readerDataStore.setFirstMushafLaunchCompleted() }

    override suspend fun setDownloadOverWifiOnly(enabled: Boolean): Result<Unit> =
        resultOf { readerDataStore.setDownloadOverWifiOnly(enabled) }

    // ── RecitationSettingsRepository ──────────────────────────────────────

    override val settings: Flow<RecitationSettings> = recitationSettingsDataStore.settings

    override suspend fun update(settings: RecitationSettings): Result<Unit> =
        resultOf { recitationSettingsDataStore.update(settings) }
}
