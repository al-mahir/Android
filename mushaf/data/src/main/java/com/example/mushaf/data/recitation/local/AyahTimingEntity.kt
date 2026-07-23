package com.example.mushaf.data.recitation.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ayah_timings")
data class AyahTimingEntity(
    @PrimaryKey
    val id: String, // format: "reciterId_verseKey" e.g., "7_1:1"
    val reciterId: Int,
    val verseKey: String, // e.g., "1:1"
    val timestampFrom: Long,
    val timestampTo: Long,
    val audioUrl: String, // remote url
    val localAudioPath: String?, // path if downloaded
    val segmentsJson: String // JSON string of segments
)
