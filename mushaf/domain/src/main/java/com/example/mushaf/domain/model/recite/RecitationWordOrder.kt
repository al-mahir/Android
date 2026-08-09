package com.example.mushaf.domain.model.recite

/**
 * A total reading order over `sura:aya:word` ids ([RecitationCursor.wordId]).
 *
 * The live highlight has two independent sources — the server's confirmed cursor and the
 * on-device predicted one — and merging them needs "is A further along than B?" for ids that may
 * sit on different ayahs or even different pages, so a page-local index lookup isn't enough.
 * Ordering straight off the id keeps this pure and page-independent.
 */
object RecitationWordOrder {

    /** A sortable key, or `null` if [wordId] isn't a well-formed `sura:aya:word` id. */
    fun keyOf(wordId: String?): Long? {
        val cursor = wordId?.let(RecitationCursor::fromWordId) ?: return null
        return cursor.sura * SURA_STRIDE + cursor.aya * AYA_STRIDE + cursor.wordIndex
    }

    /**
     * True when [candidate] comes strictly later in the Qur'an than [reference]. An unparseable or
     * absent [candidate] is never "after" anything; an absent [reference] is treated as the very
     * beginning, so the first position of a session always counts as forward progress.
     */
    fun isAfter(candidate: String?, reference: String?): Boolean {
        val candidateKey = keyOf(candidate) ?: return false
        val referenceKey = keyOf(reference) ?: return true
        return candidateKey > referenceKey
    }

    private const val AYA_STRIDE = 1_000L
    private const val SURA_STRIDE = 1_000_000L
}
