package com.example.mushaf.domain.model.recite







 
data class RecitationCursor(
    val sura: Int,
    val aya: Int,
    val wordIndex: Int = 0,
) {
     
    val wordId: String get() = "$sura:$aya:${wordIndex + 1}"

    companion object {
        





 
        fun fromWordId(wordId: String): RecitationCursor? {
            val parts = wordId.split(':')
            if (parts.size != 3) return null
            val sura = parts[0].toIntOrNull() ?: return null
            val aya = parts[1].toIntOrNull() ?: return null
            val oneBasedWord = parts[2].toIntOrNull() ?: return null
            if (sura < 1 || aya < 1 || oneBasedWord < 1) return null
            return RecitationCursor(sura, aya, oneBasedWord - 1)
        }
    }
}






 
data class RecitationCandidate(
    val start: RecitationCursor,
    val end: RecitationCursor?,
    val text: String?,
)
