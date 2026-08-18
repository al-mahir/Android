package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.repository.AyahNoteRepository

class DeleteAyahNoteUseCase(
    private val repository: AyahNoteRepository,
) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int) {
        repository.delete(surahNumber, ayahNumber)
    }
}
