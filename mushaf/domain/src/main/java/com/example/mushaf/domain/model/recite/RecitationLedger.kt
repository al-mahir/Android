package com.example.mushaf.domain.model.recite

/**
 * Accumulates per-word feedback across the chunks of a session.
 *
 * A session grades in chunks, and **the same word can appear in more than one of them** — a word
 * on a chunk boundary is reported by both the chunk that ends there and the chunk that begins
 * there. Later-wins is therefore not good enough on its own.
 *
 * The rule that matters: **an unscored report must never erase a scored one.** `docs/API.md` §5.4
 * and §5.5 show exactly this pair — word 1:1:3 comes back scored `correct` in the first chunk of
 * a Fātiḥa session and `correct, trimmed` in the second. A naive overwrite would downgrade a word
 * the model actually verified to "not checked", and the reciter would watch a verdict they had
 * already earned disappear.
 */
fun Map<String, RecitationWordFeedback>.mergedWith(
    chunk: RecitationChunk,
): Map<String, RecitationWordFeedback> = mergedWith(chunk.wordFeedbackById())

/** See [mergedWith]. */
fun Map<String, RecitationWordFeedback>.mergedWith(
    incoming: Map<String, RecitationWordFeedback>,
): Map<String, RecitationWordFeedback> {
    if (incoming.isEmpty()) return this
    val merged = toMutableMap()
    for ((wordId, word) in incoming) {
        val existing = merged[wordId]
        // A trimmed report carries no information about the recitation, so it may fill a gap
        // but never overwrite a verdict.
        if (word.isTrimmed && existing != null && !existing.isTrimmed) continue
        merged[wordId] = word
    }
    return merged
}
