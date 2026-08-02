package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.AyahNote
import kotlinx.coroutines.flow.Flow

interface AyahNoteRepository {

    fun observeNote(surahNumber: Int, ayahNumber: Int): Flow<AyahNote?>

    fun observeNotes(surahNumber: Int): Flow<List<AyahNote>>

    suspend fun upsert(note: AyahNote)

    suspend fun delete(surahNumber: Int, ayahNumber: Int)
}
