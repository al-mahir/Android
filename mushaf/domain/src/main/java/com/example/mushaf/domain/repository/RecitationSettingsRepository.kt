package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.RecitationSettings
import kotlinx.coroutines.flow.Flow

interface RecitationSettingsRepository {

    val settings: Flow<RecitationSettings>

    suspend fun update(settings: RecitationSettings)
}
