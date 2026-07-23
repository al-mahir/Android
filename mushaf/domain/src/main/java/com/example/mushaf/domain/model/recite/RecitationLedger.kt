package com.example.mushaf.domain.model.recite













 
fun Map<String, RecitationWordFeedback>.mergedWith(
    chunk: RecitationChunk,
): Map<String, RecitationWordFeedback> = mergedWith(chunk.wordFeedbackById())

 
fun Map<String, RecitationWordFeedback>.mergedWith(
    incoming: Map<String, RecitationWordFeedback>,
): Map<String, RecitationWordFeedback> {
    if (incoming.isEmpty()) return this
    val merged = toMutableMap()
    for ((wordId, word) in incoming) {
        val existing = merged[wordId]
        
        
        if (word.isTrimmed && existing != null && !existing.isTrimmed) continue
        merged[wordId] = word
    }
    return merged
}
