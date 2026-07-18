package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.LastReadSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// For now, this is a mock. It should be injected and read from Room or DataStore.
class GetLastReadUseCase {
    operator fun invoke(): Flow<LastReadSession?> {
        return flowOf(
            LastReadSession(
                surahName = "Al-Kahf",
                ayah = 45,
                page = 298
            )
        )
    }
}
