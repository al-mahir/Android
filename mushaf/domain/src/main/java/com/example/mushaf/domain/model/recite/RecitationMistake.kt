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
) {
     
    val isUnscored: Boolean get() = confidence == null
}
