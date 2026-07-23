package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.recite.RecitationSchema
import com.example.mushaf.domain.model.recite.RecitationSettings
import com.example.mushaf.domain.repository.RecitationSchemaRepository
import com.example.mushaf.domain.repository.RecitationSettingsRepository
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow

class ObserveRecitationSettingsUseCase(
    private val repository: RecitationSettingsRepository,
) {
    operator fun invoke(): Flow<RecitationSettings> = repository.settings
}

class UpdateRecitationSettingsUseCase(
    private val repository: RecitationSettingsRepository,
) {
    suspend operator fun invoke(settings: RecitationSettings) = repository.update(settings)
}

class GetRecitationSchemaUseCase(
    private val repository: RecitationSchemaRepository,
) {
    suspend operator fun invoke(): Result<RecitationSchema> = repository.schema()
}
