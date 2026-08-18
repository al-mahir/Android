package com.example.mushaf.data.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AyahNoteDao {

    @Query("SELECT * FROM ayah_notes WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    fun observeNote(surahNumber: Int, ayahNumber: Int): Flow<AyahNoteEntity?>

    @Query("SELECT * FROM ayah_notes WHERE surahNumber = :surahNumber ORDER BY ayahNumber ASC")
    fun observeNotes(surahNumber: Int): Flow<List<AyahNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: AyahNoteEntity)

    @Query("DELETE FROM ayah_notes WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun delete(surahNumber: Int, ayahNumber: Int)
}
