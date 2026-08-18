package com.example.mushaf.data.repository

import com.example.mushaf.data.notes.AyahNoteDao
import com.example.mushaf.domain.model.AyahNote
import com.example.mushaf.domain.repository.AyahNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AyahNotesRepositoryImpl(
    private val dao: AyahNoteDao,
) : AyahNoteRepository {

    override fun observeNote(surahNumber: Int, ayahNumber: Int): Flow<AyahNote?> =
        dao.observeNote(surahNumber, ayahNumber).map { it?.toDomain() }

    override fun observeNotes(surahNumber: Int): Flow<List<AyahNote>> =
        dao.observeNotes(surahNumber).map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(note: AyahNote) {
        dao.upsert(
            com.example.mushaf.data.notes.AyahNoteEntity(
                surahNumber = note.surahNumber,
                ayahNumber = note.ayahNumber,
                text = note.text,
                updatedAtEpochMs = note.updatedAtEpochMs,
            ),
        )
    }

    override suspend fun delete(surahNumber: Int, ayahNumber: Int) {
        dao.delete(surahNumber, ayahNumber)
    }

    private fun com.example.mushaf.data.notes.AyahNoteEntity.toDomain() = AyahNote(
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        text = text,
        updatedAtEpochMs = updatedAtEpochMs,
    )
}
