package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.AyahNote
import com.example.mushaf.domain.repository.AyahNoteRepository

class UpsertAyahNoteUseCase(
    private val repository: AyahNoteRepository,
) {
    suspend operator fun invoke(
        surahNumber: Int,
        ayahNumber: Int,
        text: String,
        nowEpochMs: Long = System.currentTimeMillis(),
    ) {
        repository.upsert(
            AyahNote(
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                text = text.trim(),
                updatedAtEpochMs = nowEpochMs,
            ),
        )
    }
}
