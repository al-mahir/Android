package com.example.mushaf.domain.model.recite

 
enum class RecitationWordStatus {
    CORRECT,
     
    ALMOST,
    ERROR,
}








 
enum class RecitationWordMark {
     
    CORRECT,

    





 
    HINT,

     
    MISTAKE,

    




 
    UNVERIFIED,
}





 
data class RecitationWordFeedback(
    val position: RecitationCursor,
    val uthmani: String,
    val status: RecitationWordStatus,
    val mistakes: List<RecitationMistake>,
    val isTrimmed: Boolean,
) {
     
    val wordId: String get() = position.wordId

    


 
    val mark: RecitationWordMark
        get() = when {
            isTrimmed -> RecitationWordMark.UNVERIFIED
            status == RecitationWordStatus.CORRECT -> RecitationWordMark.CORRECT
            status == RecitationWordStatus.ALMOST -> RecitationWordMark.HINT
            else -> RecitationWordMark.MISTAKE
        }

    



 
    val countsAsMistake: Boolean get() = mark == RecitationWordMark.MISTAKE

     
    val scorableMistakes: List<RecitationMistake>
        get() = if (countsAsMistake) mistakes else emptyList()
}
