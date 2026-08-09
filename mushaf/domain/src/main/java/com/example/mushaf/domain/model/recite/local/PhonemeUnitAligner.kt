package com.example.mushaf.domain.model.recite.local

/**
 * Recovers which Mushaf word(s) each phoneme unit of an ayah covers.
 *
 * The reference table (`ordered_quran_phonemes.json`) stores an ayah's pronunciation as a list of
 * units, but tajwīd merges adjacent words, so that list is shorter than the word list for most
 * ayahs and a positional `units[i] -> words[i]` mapping desyncs immediately. What holds is weaker
 * but enough: units appear in reading order, each covers one or more *consecutive* words, and no
 * word is ever split across two units. That makes the mapping a monotone segmentation, solvable
 * exactly by dynamic programming over "which words does unit j start at".
 *
 * Cost of a candidate grouping is the edit distance between the unit's phoneme skeleton and the
 * concatenated skeleton of the words it would cover ([QuranPhonemeNormalizer.skeleton] puts the
 * two scripts in the same alphabet). A merged unit scores far better against both its words than
 * against either one alone, which is exactly the signal needed to place the merge.
 */
object PhonemeUnitAligner {

    /**
     * [units] must be the complete unit list for one ayah and [words] its complete word list, in
     * reading order — a partial window can't be aligned, since a merge that starts before the
     * window would shift everything after it. Returns an empty list when the two can't describe
     * the same ayah (more units than words, or either side empty), so callers degrade to "no local
     * tracking here" rather than to a wrong mapping.
     */
    fun align(units: List<String>, words: List<LocalWordEntry>): List<ReferencePhonemeUnit> {
        if (units.isEmpty() || words.isEmpty() || units.size > words.size) return emptyList()

        val unitSkeletons = units.map(QuranPhonemeNormalizer::skeleton)
        val wordSkeletons = words.map { QuranPhonemeNormalizer.skeleton(it.plainText) }

        // dp[i][j] = cheapest way to cover the first i words with the first j units.
        val unreachable = Int.MAX_VALUE / 2
        val dp = Array(words.size + 1) { IntArray(units.size + 1) { unreachable } }
        val groupSize = Array(words.size + 1) { IntArray(units.size + 1) }
        dp[0][0] = 0

        for (wordCount in 1..words.size) {
            for (unitCount in 1..minOf(unitSkeletons.size, wordCount)) {
                val maxSpan = minOf(MAX_WORDS_PER_UNIT, wordCount)
                for (span in 1..maxSpan) {
                    val previous = dp[wordCount - span][unitCount - 1]
                    if (previous >= unreachable) continue
                    val spelled = buildString {
                        for (index in wordCount - span until wordCount) append(wordSkeletons[index])
                    }
                    val cost = previous + EditDistance.between(spelled, unitSkeletons[unitCount - 1])
                    if (cost < dp[wordCount][unitCount]) {
                        dp[wordCount][unitCount] = cost
                        groupSize[wordCount][unitCount] = span
                    }
                }
            }
        }

        if (dp[words.size][units.size] >= unreachable) return emptyList()

        val aligned = ArrayList<ReferencePhonemeUnit>(units.size)
        var wordCount = words.size
        var unitCount = units.size
        while (unitCount > 0) {
            val span = groupSize[wordCount][unitCount]
            if (span <= 0) return emptyList()
            aligned += ReferencePhonemeUnit(
                phonemes = units[unitCount - 1],
                wordIds = words.subList(wordCount - span, wordCount).map { it.wordId },
            )
            wordCount -= span
            unitCount -= 1
        }
        aligned.reverse()
        return aligned
    }

    /** Tajwīd joins a word to its neighbour, occasionally to two; beyond that a "merge" is far
     * more likely to be a corrupt row than a real pronunciation, and the wider span only makes the
     * segmentation easier to get wrong. */
    private const val MAX_WORDS_PER_UNIT = 4
}
