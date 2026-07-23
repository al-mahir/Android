package com.iti.data.local.recitation

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable


@Entity(tableName = "recitation_sessions")
data class RecitationSessionEntity(
    @PrimaryKey val id: String,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val startSura: Int,
    val startAya: Int,
    val endSura: Int,
    val endAya: Int,
    val scoredWordCount: Int,
    /** JSON array of [StoredMistake]. */
    val mistakesJson: String,
    /** JSON array of [StoredPracticeFocus]. */
    val practiceFocusJson: String,
)


@Serializable
data class StoredMistake(
    val sura: Int,
    val aya: Int,
    val wordIndex: Int,
    val word: String,
    val category: String,
    val ruleName: String? = null,
    val expectedLength: Int? = null,
    val actualLength: Int? = null,
)

@Serializable
data class StoredPracticeFocus(
    val category: String,
    val ruleName: String? = null,
    val occurrences: Int,
)
