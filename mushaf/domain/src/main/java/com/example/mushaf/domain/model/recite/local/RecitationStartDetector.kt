package com.example.mushaf.domain.model.recite.local

/**
 * Figures out which word in a page the reciter is actually starting from, from the first few
 * settled words a local speech recognizer produces — so a live session doesn't have to assume
 * "the first word of the page" the way seeding straight from the page start would. Text-only,
 * same as [ArabicPhoneticMatcher]: no acoustic confidence, so a single matching word is treated
 * as ambiguous on purpose (short/common words repeat across a page) and this waits for
 * [minChainLength] *consecutive* words to agree on one position before committing, giving up
 * after [maxWordsConsidered] rather than listening forever.
 */
class RecitationStartDetector(
    private val window: List<LocalWordEntry>,
    private val minChainLength: Int = 2,
    private val maxWordsConsidered: Int = 6,
) {
    private data class Candidate(val startIndex: Int, val nextExpectedIndex: Int, val length: Int)

    private var candidates: List<Candidate> = emptyList()
    private var wordsConsidered = 0
    private var decided = false

    /**
     * Feed one settled spoken word. Returns the detected starting word once this has decided —
     * either a confident lock, or its best guess after giving up — `null` means "keep offering
     * words." Once non-null has been returned, further calls are no-ops.
     */
    fun offer(spokenWord: String): LocalWordEntry? {
        if (decided || window.isEmpty()) return null
        wordsConsidered++

        // Chains already in progress: do they keep matching the next word in sequence?
        val extended = candidates.mapNotNull { candidate ->
            val next = window.getOrNull(candidate.nextExpectedIndex) ?: return@mapNotNull null
            if (ArabicPhoneticMatcher.isMatch(spokenWord, next.plainText)) {
                candidate.copy(nextExpectedIndex = candidate.nextExpectedIndex + 1, length = candidate.length + 1)
            } else {
                null
            }
        }

        // This word is also always a fresh potential start on its own, covering a misheard or
        // ignored first word not invalidating every later chain.
        val fresh = window.indices
            .filter { index -> ArabicPhoneticMatcher.isMatch(spokenWord, window[index].plainText) }
            .map { index -> Candidate(startIndex = index, nextExpectedIndex = index + 1, length = 1) }

        candidates = (extended + fresh).distinctBy { it.startIndex }

        val locked = candidates.filter { it.length >= minChainLength }
        if (locked.size == 1) {
            decided = true
            return window.getOrNull(locked.first().startIndex)
        }

        if (wordsConsidered >= maxWordsConsidered) {
            decided = true
            val best = (locked.ifEmpty { candidates }).minByOrNull { it.startIndex }
            return best?.let { window.getOrNull(it.startIndex) } ?: window.first()
        }

        return null
    }
}
