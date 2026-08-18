package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.AyahNote
import com.example.mushaf.domain.repository.AyahNoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveAyahNoteUseCase(
    private val repository: AyahNoteRepository,
) {
    operator fun invoke(surahNumber: Int, ayahNumber: Int): Flow<AyahNote?> =
        repository.observeNote(surahNumber, ayahNumber)
}
