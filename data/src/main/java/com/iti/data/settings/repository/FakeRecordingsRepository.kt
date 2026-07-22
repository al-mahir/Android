package com.iti.data.settings.repository

import com.iti.domain.settings.repository.RecordingsRepository
import kotlinx.coroutines.delay


class FakeRecordingsRepository : RecordingsRepository {

    override suspend fun deleteAll() {
        delay(FAKE_DELETE_MILLIS)
    }

    private companion object {
        const val FAKE_DELETE_MILLIS = 900L
    }
}
