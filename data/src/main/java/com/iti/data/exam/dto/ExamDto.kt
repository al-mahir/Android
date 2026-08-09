package com.iti.data.exam.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class StartSessionDto(
    @SerialName("type") val type: String = "start",
    @SerialName("sura") val sura: Int,
    @SerialName("aya") val aya: Int,
    @SerialName("strictness") val strictness: String = "normal",
)

@Serializable
data class EndSessionDto(
    @SerialName("type") val type: String = "end",
)

@Serializable
data class SeekDto(
    @SerialName("type") val type: String = "seek",
    @SerialName("sura") val sura: Int,
    @SerialName("aya") val aya: Int,
    @SerialName("word_idx") val wordIdx: Int = 0,
)

@Serializable
data class IncomingMessageDto(
    @SerialName("type") val type: String,
)

@Serializable
data class SessionAckDto(
    @SerialName("type") val type: String,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("engine") val engine: String? = null,
    @SerialName("sample_rate") val sampleRate: Int? = null,
)

/**
 * The nested feedback payload inside a "feedback" event.
 * The API sends words/status/uthmani_text inside `feedback.feedback`, not at top level.
 */
@Serializable
data class FeedbackPayloadDto(
    @SerialName("status") val status: String = "ok",
    @SerialName("uthmani_text") val uthmaniText: String? = null,
    @SerialName("words") val words: List<WordDto> = emptyList(),
    @SerialName("span") val span: CursorDto? = null,
    @SerialName("end") val end: CursorDto? = null,
    @SerialName("candidates") val candidates: List<CandidateDto> = emptyList(),
    @SerialName("non_verse") val nonVerse: List<String> = emptyList(),
)

@Serializable
data class CandidateDto(
    @SerialName("sura") val sura: Int,
    @SerialName("aya") val aya: Int,
    @SerialName("word_idx") val wordIdx: Int = 0,
    @SerialName("uthmani_text") val uthmaniText: String? = null,
)

/**
 * Top-level feedback event frame. The graded words are inside [feedback].
 * `cursor` is the server's updated reading position after this chunk.
 */
@Serializable
data class FeedbackDto(
    @SerialName("type") val type: String,
    @SerialName("chunk_seq") val chunkSeq: Int = 0,
    @SerialName("forced_cut") val forcedCut: Boolean = false,
    @SerialName("feedback") val feedback: FeedbackPayloadDto? = null,
    @SerialName("cursor") val cursor: CursorDto? = null,
)

/**
 * Word-level grading result. Field name is "uthmani" in the API (not "word").
 */
@Serializable
data class WordDto(
    @SerialName("uthmani") val uthmani: String,
    @SerialName("status") val status: String,
    @SerialName("errors") val errors: List<ErrorDto> = emptyList(),
    @SerialName("trimmed") val trimmed: Boolean = false,
    @SerialName("word_idx") val wordIdx: Int = 0,
    @SerialName("sura") val sura: Int? = null,
    @SerialName("aya") val aya: Int? = null,
)

/**
 * A single grading finding on a word.
 * [errorType] = "tajweed" | "normal" | "tashkeel" | "sifa"
 * [speechErrorType] = "insert" | "delete" | "replace"
 */
@Serializable
data class ErrorDto(
    @SerialName("error_type") val errorType: String? = null,
    @SerialName("speech_error_type") val speechErrorType: String? = null,
    @SerialName("tajweed_rules") val tajweedRules: List<TajweedRuleDetailDto> = emptyList(),
    @SerialName("expected_ph") val expectedPh: String? = null,
    @SerialName("predicted_ph") val predictedPh: String? = null,
    @SerialName("confidence") val confidence: Double? = null,
    // Legacy / fallback fields kept for compat
    @SerialName("type") val type: String? = null,
    @SerialName("rule") val rule: TajweedRuleDto? = null,
    @SerialName("expected") val expected: String? = null,
    @SerialName("predicted") val predicted: String? = null,
)

/** Detail of a tajweed rule that was violated. */
@Serializable
data class TajweedRuleDetailDto(
    @SerialName("name_ar") val nameAr: String? = null,
    @SerialName("name_en") val nameEn: String? = null,
    @SerialName("golden_len") val goldenLen: Int? = null,
    @SerialName("correctness_type") val correctnessType: String? = null,
    @SerialName("tag") val tag: String? = null,
)

/** Legacy tajweed rule reference used in older error format. */
@Serializable
data class TajweedRuleDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("name_ar") val nameAr: String? = null,
)

@Serializable
data class CursorDto(
    @SerialName("sura") val sura: Int,
    @SerialName("aya") val aya: Int,
    @SerialName("word_idx") val word: Int = 0,
)

/** The "done" event arrives after `{"type":"end"}` is sent. */
@Serializable
data class DoneDto(
    @SerialName("type") val type: String,
    @SerialName("cursor") val cursor: CursorDto? = null,
)
