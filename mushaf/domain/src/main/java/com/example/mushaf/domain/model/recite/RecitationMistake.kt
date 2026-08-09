package com.example.mushaf.domain.model.recite







 
enum class MistakeCategory {
     
    MEMORIZATION,

     
    TASHKIL,

     
    TAJWID,

     
    OTHER,
}

 
enum class SpeechErrorType {
    INSERT,
    DELETE,
    REPLACE,
    UNKNOWN,
}




 
data class TajweedRuleReference(
    val nameArabic: String,
    val nameEnglish: String?,
    val goldenLength: Int?,
     
    val correctnessType: String?,
    val tag: String?,
)







 
/**
 * A ṣifā finding expressed as an attribute comparison.
 *
 * On the `sifa` channel the engine does not put phonemes in the phoneme fields — it puts
 * `attribute=value` tokens such as `shidda_or_rakhawa=shadeed`. That is wire diagnostics, not
 * something a reciter can read, so the data layer parses it apart here and the UI names both
 * halves in the reader's language.
 *
 * At least one of [expectedValue] / [actualValue] is non-null; a pure deletion has no actual.
 */
data class SifaComparison(
    /** Attribute key as sent by the engine, e.g. `shidda_or_rakhawa`. */
    val attributeKey: String,
    val expectedValue: String?,
    val actualValue: String?,
)

data class RecitationMistake(
    val category: MistakeCategory,

    val rawChannel: String,
    val speechErrorType: SpeechErrorType,
    



 
    val uthmaniSpan: IntRange?,
    val expectedPhonemes: String?,
    val predictedPhonemes: String?,
     
    val expectedLength: Int?,
    val actualLength: Int?,
    val rules: List<TajweedRuleReference>,
    val confidence: Float?,
    /** Set only on the `sifa` channel, where the phoneme fields carry `attribute=value` instead. */
    val sifa: SifaComparison? = null,
) {
     
    val isUnscored: Boolean get() = confidence == null
}
