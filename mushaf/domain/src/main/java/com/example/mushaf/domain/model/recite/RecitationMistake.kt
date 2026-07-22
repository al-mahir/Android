package com.example.mushaf.domain.model.recite

/**
 * The shared mistake taxonomy (SDD §4 `almahir.v1`), which the service's four error channels
 * fold into.
 *
 * Tashkīl and tajwīd stay separate because they are scored independently (ACC-04) — collapsing
 * them would make it impossible to tell a learner whether their vowels or their rules need work.
 */
enum class MistakeCategory {
    /** Ḥifẓ: a wrong, missing or extra word. The service calls this channel `normal`. */
    MEMORIZATION,

    /** A wrong or missing ḥaraka. */
    TASHKIL,

    /** A tajwīd rule or an articulation attribute (ṣifā). */
    TAJWID,

    /** A channel this client does not know. Kept so a server-side addition is never miscounted. */
    OTHER,
}

/** What the reciter did, relative to the reference. */
enum class SpeechErrorType {
    INSERT,
    DELETE,
    REPLACE,
    UNKNOWN,
}

/**
 * A rule a finding touches, ready to explain the mistake:
 * "المد الطبيعي: expected 2, you held 3."
 */
data class TajweedRuleReference(
    val nameArabic: String,
    val nameEnglish: String?,
    val goldenLength: Int?,
    /** `match` or `count`; count-based rules are the ones with a meaningful length. */
    val correctnessType: String?,
    val tag: String?,
)

/**
 * One finding against a word.
 *
 * [confidence] is null for **unscored**, which is not the same as certain — the service grades a
 * null-confidence finding as a hint at every strictness level, `strict` included. Anything here
 * that derives a score must treat null as "no evidence", never as 1.0.
 */
data class RecitationMistake(
    val category: MistakeCategory,
    /** The service's raw `error_type`, kept so a detail view can be exact about the channel. */
    val rawChannel: String,
    val speechErrorType: SpeechErrorType,
    /**
     * Character span in the Uthmani text — **the** field for inline highlighting. The two phoneme
     * spans use different coordinate systems and diverge on every error worth scoring, so they
     * are diagnostic only and deliberately not carried here.
     */
    val uthmaniSpan: IntRange?,
    val expectedPhonemes: String?,
    val predictedPhonemes: String?,
    /** For count-based rules: the correct number of ḥarakāt, and how many were held. */
    val expectedLength: Int?,
    val actualLength: Int?,
    val rules: List<TajweedRuleReference>,
    val confidence: Float?,
) {
    /** True when the model reported no probability for this finding. */
    val isUnscored: Boolean get() = confidence == null
}
