package com.example.mushaf.data.repository

import com.example.mushaf.data.prefs.RecitationSettingsDataStore
import com.example.mushaf.domain.model.recite.RecitationSettings
import com.example.mushaf.domain.repository.RecitationSettingsRepository
import kotlinx.coroutines.flow.Flow

class RecitationSettingsRepositoryImpl(
    private val dataStore: RecitationSettingsDataStore,
) : RecitationSettingsRepository {

    override val settings: Flow<RecitationSettings> = dataStore.settings

    override suspend fun update(settings: RecitationSettings) = dataStore.update(settings)
}
