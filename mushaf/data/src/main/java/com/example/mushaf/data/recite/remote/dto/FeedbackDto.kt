package com.example.mushaf.data.recite.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

 
@Serializable
data class PositionDto(
    val sura: Int,
    val aya: Int,
    @SerialName("word_idx") val wordIdx: Int = 0,
)




 
@Serializable
data class FeedbackEnvelopeDto(
    val type: String,
    @SerialName("chunk_seq") val chunkSeq: Int,
     
    @SerialName("audio_span_sec") val audioSpanSec: List<Double> = emptyList(),
     
    @SerialName("forced_cut") val forcedCut: Boolean = false,
    val phonemes: String? = null,
    val feedback: FeedbackDto? = null,
     
    val cursor: PositionDto? = null,
)






 
@Serializable
data class FeedbackDto(
     
    val status: String,
    val span: PositionDto? = null,
    val end: PositionDto? = null,
    @SerialName("uthmani_text") val uthmaniText: String? = null,
    @SerialName("predicted_phonemes") val predictedPhonemes: String? = null,
    @SerialName("reference_phonemes") val referencePhonemes: String? = null,
    val words: List<FeedbackWordDto> = emptyList(),
     
    val candidates: List<CandidateDto> = emptyList(),
     
    @SerialName("non_verse") val nonVerse: List<String> = emptyList(),
)

@Serializable
data class CandidateDto(
    val sura: Int,
    val aya: Int,
    @SerialName("word_idx") val wordIdx: Int = 0,
    @SerialName("uthmani_text") val uthmaniText: String? = null,
    val end: PositionDto? = null,
)






 
@Serializable
data class FeedbackWordDto(
    val sura: Int,
    val aya: Int,
    @SerialName("word_idx") val wordIdx: Int,
    val uthmani: String = "",
     
    val status: String,
    val errors: List<WordErrorDto> = emptyList(),
     
    val trimmed: Boolean = false,
)

 
@Serializable
data class WordErrorDto(
     
    @SerialName("error_type") val errorType: String,
     
    @SerialName("speech_error_type") val speechErrorType: String? = null,
     
    @SerialName("uthmani_pos") val uthmaniPos: List<Int>? = null,
     
    @SerialName("ph_pos") val phPos: List<Int>? = null,
     
    @SerialName("pred_ph_pos") val predPhPos: List<Int>? = null,
    @SerialName("expected_ph") val expectedPh: String? = null,
    @SerialName("predicted_ph") val predictedPh: String? = null,
    @SerialName("expected_len") val expectedLen: Int? = null,
    @SerialName("predicted_len") val predictedLen: Int? = null,
    @SerialName("tajweed_rules") val tajweedRules: List<TajweedRuleRefDto> = emptyList(),
    


 
    val confidence: Float? = null,
)

 
@Serializable
data class TajweedRuleRefDto(
    @SerialName("name_ar") val nameAr: String,
    @SerialName("name_en") val nameEn: String? = null,
    @SerialName("golden_len") val goldenLen: Int? = null,
     
    @SerialName("correctness_type") val correctnessType: String? = null,
    val tag: String? = null,
)
