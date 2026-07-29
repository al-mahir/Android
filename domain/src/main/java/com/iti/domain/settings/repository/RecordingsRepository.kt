package com.iti.domain.settings.repository

import com.iti.domain.core.Result


interface RecordingsRepository {

    suspend fun deleteAll(): Result<Unit>
}
