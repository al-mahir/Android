package com.example.mushaf.data.recite.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A muṣḥaf coordinate. [sura] and [aya] are 1-based; [wordIdx] is 0-based within the āyah. */
@Serializable
data class PositionDto(
    val sura: Int,
    val aya: Int,
    @SerialName("word_idx") val wordIdx: Int = 0,
)

/**
 * One feedback event, pushed per finalized waqf chunk — not polled, and not one per frame sent.
 * Silence produces no event at all, so nothing here should drive a timeout.
 */
@Serializable
data class FeedbackEnvelopeDto(
    val type: String,
    @SerialName("chunk_seq") val chunkSeq: Int,
    /** Start and end of this chunk in *streamed* audio time. */
    @SerialName("audio_span_sec") val audioSpanSec: List<Double> = emptyList(),
    /** True when the 19 s cap ended the chunk rather than a pause; nearby words are less reliable. */
    @SerialName("forced_cut") val forcedCut: Boolean = false,
    val phonemes: String? = null,
    val feedback: FeedbackDto? = null,
    /** Where the reciter now is. Keep the latest — it is the resume point after a dropped socket. */
    val cursor: PositionDto? = null,
)

/**
 * The graded result.
 *
 * [status] is a gate, not a label: on `ambiguous` or `no_match` the service is declining to
 * guess, and nothing may be asserted against the reciter.
 */
@Serializable
data class FeedbackDto(
    /** `ok` | `ambiguous` | `no_match`. */
    val status: String,
    val span: PositionDto? = null,
    val end: PositionDto? = null,
    @SerialName("uthmani_text") val uthmaniText: String? = null,
    @SerialName("predicted_phonemes") val predictedPhonemes: String? = null,
    @SerialName("reference_phonemes") val referencePhonemes: String? = null,
    val words: List<FeedbackWordDto> = emptyList(),
    /** Populated only when [status] is `ambiguous`. Each carries its text, not just coordinates. */
    val candidates: List<CandidateDto> = emptyList(),
    /** Recognised `istiaatha` / `basmalah` / `sadaka`, excluded from scoring but not hidden. */
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

/**
 * Per-word feedback.
 *
 * Read [trimmed] **before** trusting [status]: a trimmed word was cut by the chunker and not
 * scored at all, so a `correct` status on it asserts nothing.
 */
@Serializable
data class FeedbackWordDto(
    val sura: Int,
    val aya: Int,
    @SerialName("word_idx") val wordIdx: Int,
    val uthmani: String = "",
    /** `correct` | `almost` | `error`. `almost` is a hint, never a mistake. */
    val status: String,
    val errors: List<WordErrorDto> = emptyList(),
    /** True means the word sat on a chunk boundary and was **not scored**. */
    val trimmed: Boolean = false,
)

/** A single finding against a word. */
@Serializable
data class WordErrorDto(
    /** `tajweed` | `normal` | `tashkeel` | `sifa`. `normal` is ḥifẓ: a wrong or missing word. */
    @SerialName("error_type") val errorType: String,
    /** `insert` | `delete` | `replace`. */
    @SerialName("speech_error_type") val speechErrorType: String? = null,
    /** Character span in the Uthmani text — the field to use for inline highlighting. */
    @SerialName("uthmani_pos") val uthmaniPos: List<Int>? = null,
    /** Span in the reference phoneme string. Diagnostic only; a different coordinate system. */
    @SerialName("ph_pos") val phPos: List<Int>? = null,
    /** Span in the predicted string. Null for a pure deletion. */
    @SerialName("pred_ph_pos") val predPhPos: List<Int>? = null,
    @SerialName("expected_ph") val expectedPh: String? = null,
    @SerialName("predicted_ph") val predictedPh: String? = null,
    @SerialName("expected_len") val expectedLen: Int? = null,
    @SerialName("predicted_len") val predictedLen: Int? = null,
    @SerialName("tajweed_rules") val tajweedRules: List<TajweedRuleRefDto> = emptyList(),
    /**
     * Model confidence, or null for **unscored** — which is not the same as certain. A null
     * grades `almost` at every strictness level, `strict` included.
     */
    val confidence: Float? = null,
)

/** The rule a finding touches, ready to explain the mistake to the reciter. */
@Serializable
data class TajweedRuleRefDto(
    @SerialName("name_ar") val nameAr: String,
    @SerialName("name_en") val nameEn: String? = null,
    @SerialName("golden_len") val goldenLen: Int? = null,
    /** `match` or `count`. */
    @SerialName("correctness_type") val correctnessType: String? = null,
    val tag: String? = null,
)
