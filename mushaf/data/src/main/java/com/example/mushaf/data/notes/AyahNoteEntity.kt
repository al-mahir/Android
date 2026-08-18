package com.example.mushaf.data.notes

import androidx.room.Entity

@Entity(tableName = "ayah_notes", primaryKeys = ["surahNumber", "ayahNumber"])
data class AyahNoteEntity(
    val surahNumber: Int,
    val ayahNumber: Int,
    val text: String,
    val updatedAtEpochMs: Long,
)
