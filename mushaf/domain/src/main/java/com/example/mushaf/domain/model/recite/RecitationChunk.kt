package com.example.mushaf.domain.model.recite

 
enum class NonVerseSegment {
    ISTIAATHA,
    BASMALAH,
    SADAKA,
    OTHER,
}








 
sealed interface RecitationMatch {

     
    data class Matched(
        val words: List<RecitationWordFeedback>,
        val text: String?,
        val start: RecitationCursor?,
        val end: RecitationCursor?,
        /** Phonemes the model heard across the whole matched passage. */
        val predictedPhonemes: String? = null,
        /** Phonemes the passage should have produced. */
        val referencePhonemes: String? = null,
    ) : RecitationMatch

    




 
    data class Ambiguous(val candidates: List<RecitationCandidate>) : RecitationMatch

    


 
    data object NoMatch : RecitationMatch
}







 
data class RecitationChunk(
    val sequence: Int,
    val match: RecitationMatch,
    val cursor: RecitationCursor?,
    val forcedCut: Boolean,
    val nonVerse: List<NonVerseSegment>,
    /** What the model heard for this chunk, whether or not the passage was identified. */
    val heardPhonemes: String? = null,
) {
    /** Heard against expected for the matched passage, when the chunk matched one. */
    val predictedPhonemes: String?
        get() = (match as? RecitationMatch.Matched)?.predictedPhonemes ?: heardPhonemes

    val referencePhonemes: String?
        get() = (match as? RecitationMatch.Matched)?.referencePhonemes

     
    val words: List<RecitationWordFeedback>
        get() = (match as? RecitationMatch.Matched)?.words.orEmpty()

    


 
    val mistakeWords: List<RecitationWordFeedback>
        get() = words.filter { it.countsAsMistake }

     
    fun wordFeedbackById(): Map<String, RecitationWordFeedback> =
        words.associateBy { it.wordId }
}
