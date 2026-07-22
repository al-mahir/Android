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
) {
     
    val words: List<RecitationWordFeedback>
        get() = (match as? RecitationMatch.Matched)?.words.orEmpty()

    


 
    val mistakeWords: List<RecitationWordFeedback>
        get() = words.filter { it.countsAsMistake }

     
    fun wordFeedbackById(): Map<String, RecitationWordFeedback> =
        words.associateBy { it.wordId }
}
