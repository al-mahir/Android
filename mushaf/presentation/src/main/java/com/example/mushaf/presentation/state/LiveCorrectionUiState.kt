package com.example.mushaf.presentation.state

import com.example.mushaf.domain.model.recite.NonVerseSegment
import com.example.mushaf.domain.model.recite.RecitationCandidate
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.RecitationWordMark






 
enum class ChunkOutcome {
     
    GRADED,

     
    AMBIGUOUS,

     
    NO_MATCH,
}






 
data class LiveCorrectionUiState(
     
    val isConnecting: Boolean = false,

     
    val isActive: Boolean = false,

     
    val engine: String? = null,

    




 
    val engineSubstituted: Boolean = false,

     
    val wordFeedback: Map<String, RecitationWordFeedback> = emptyMap(),

     
    val candidates: List<RecitationCandidate> = emptyList(),

     
    val nonVerse: List<NonVerseSegment> = emptyList(),

    val lastOutcome: ChunkOutcome? = null,

     
    val cursor: RecitationCursor? = null,

     
    val selectedMistakeWordId: String? = null,
) {
    





 
    val mistakeWords: List<RecitationWordFeedback>
        get() = wordFeedback.values.filter { it.countsAsMistake }

    val mistakeCount: Int get() = mistakeWords.size

    




 
    val scoredWordCount: Int
        get() = wordFeedback.values.count { it.mark != RecitationWordMark.UNVERIFIED }

    







 
    val accuracy: Float?
        get() = scoredWordCount
            .takeIf { it > 0 }
            ?.let { (it - mistakeCount).toFloat() / it }

     
    fun feedbackFor(wordId: String): RecitationWordFeedback? = wordFeedback[wordId]

    val selectedMistake: RecitationWordFeedback?
        get() = selectedMistakeWordId?.let { wordFeedback[it] }?.takeIf { it.countsAsMistake }
}
