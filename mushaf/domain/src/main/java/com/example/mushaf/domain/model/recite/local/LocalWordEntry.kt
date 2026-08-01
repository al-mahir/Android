package com.example.mushaf.domain.model.recite.local

/** A single word's plain, ASR-comparable text, addressed by the same `sura:aya:word` id used
 * everywhere else in the recitation pipeline ([com.example.mushaf.domain.model.recite.RecitationCursor.wordId]). */
data class LocalWordEntry(
    val wordId: String,
    val plainText: String,
)
