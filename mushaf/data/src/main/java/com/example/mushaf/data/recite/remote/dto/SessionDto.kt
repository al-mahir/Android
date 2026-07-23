package com.example.mushaf.data.recite.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement







 
@Serializable
data class StartSessionDto(
    val type: String = "start",
     
    val sura: Int? = null,
     
    val aya: Int? = null,
     
    @SerialName("word_idx") val wordIdx: Int? = null,
     
    val strictness: String? = null,
    val engine: String? = null,
    


 
    val rules: List<String>? = null,
    



 
    val moshaf: Map<String, JsonElement>? = null,
     
    @SerialName("include_units") val includeUnits: Boolean? = null,
)







 
@Serializable
data class SessionAckDto(
    val type: String,
    @SerialName("session_id") val sessionId: String,
    val engine: String,
    @SerialName("sample_rate") val sampleRate: Int,
)

 
@Serializable
data class SeekDto(
    val type: String = "seek",
    val sura: Int,
    val aya: Int,
    @SerialName("word_idx") val wordIdx: Int = 0,
)

 
@Serializable
data class EndSessionDto(val type: String = "end")

 
@Serializable
data class MessageEnvelopeDto(val type: String? = null)
