package com.example.mushaf.domain.model.recite.local

/**
 * Streaming forced alignment of what the on-device model is hearing against what the reciter is
 * *supposed* to be saying next, so the highlight can move between server chunks instead of waiting
 * for them.
 *
 * The recognizer hands over a **cumulative** phoneme transcript, re-emitted every decode (~100ms),
 * with no word boundaries in it. This walks a small window of expected units ([setWindow]) and
 * consumes that transcript unit by unit: whenever the next stretch of heard phonemes looks like
 * the next expected unit, the unit is committed and its last word id is returned as the new
 * predicted position.
 *
 * Two properties callers depend on:
 * - It is **structurally incapable of guessing**. Every id it returns comes out of the window it
 *   was given, and it only ever walks forward through that window, so a wrong prediction is
 *   bounded by the window and erased by the next [setWindow] from a server-confirmed cursor.
 * - It never blocks on a miss. A unit that doesn't match simply isn't committed; the transcript
 *   keeps growing and the same unit is retried on the next decode, so a mumbled word costs
 *   nothing more than the highlight sitting still until the server corrects it.
 */
class PhonemeCursorTracker {

    private class PreparedUnit(val phonemes: String, val lastWordId: String)

    private var units: List<PreparedUnit> = emptyList()
    private var unitIndex = 0

    /** How much of the current transcript has already been attributed to committed units. */
    private var consumed = 0

    /** Length of the last transcript seen, to notice the recognizer restarting its utterance. */
    private var transcriptLength = 0

    /** The window entry last committed, or `null` before anything has been heard. */
    var currentWordId: String? = null
        private set

    /**
     * Re-anchors on a fresh span of expected units. Everything heard so far is written off as
     * belonging to the *previous* anchor — the server cursor this window is built around already
     * accounts for it — so the next [offer] starts aligning from the current end of the transcript
     * rather than replaying the whole session against a window it doesn't belong to.
     */
    fun setWindow(newUnits: List<ReferencePhonemeUnit>) {
        units = newUnits.map { PreparedUnit(QuranPhonemeNormalizer.normalize(it.phonemes), it.lastWordId) }
        unitIndex = 0
        consumed = transcriptLength
        currentWordId = null
    }

    /**
     * Offers the recognizer's current cumulative transcript. Returns the furthest word id newly
     * confirmed by it, or `null` when nothing new settled — which callers must treat as "hold the
     * position", never as "the reciter stopped".
     */
    fun offer(transcript: String): String? {
        val heard = QuranPhonemeNormalizer.normalize(transcript)
        // A shorter transcript than last time means the recognizer hit an endpoint and started a
        // new utterance; the window is still valid, only the offsets into the transcript are not.
        if (heard.length < transcriptLength) consumed = 0
        transcriptLength = heard.length

        var offset = consumed.coerceAtMost(heard.length)
        var settled: String? = null
        while (unitIndex < units.size) {
            val unit = units[unitIndex]
            val taken = consumeUnit(heard, offset, unit.phonemes) ?: break
            offset += taken
            settled = unit.lastWordId
            unitIndex++
        }
        consumed = offset
        if (settled != null) currentWordId = settled
        return settled
    }

    fun reset() {
        units = emptyList()
        unitIndex = 0
        consumed = 0
        transcriptLength = 0
        currentWordId = null
    }

    /**
     * How many characters of `heard` from [from] were spent on [unit], or `null` if the unit
     * hasn't been said (or hasn't been said *yet* — the common case, since this runs every 100ms
     * and most of those decodes land mid-word).
     *
     * Requiring at least a unit's worth of audio before committing is what keeps the highlight
     * from running ahead on a prefix: `رَحمَان` would otherwise match the opening of `رَح…` and
     * jump a word early on every long word.
     */
    private fun consumeUnit(heard: String, from: Int, unit: String): Int? {
        if (unit.isEmpty()) return null
        val available = heard.length - from
        if (available < unit.length) return null

        val slack = (unit.length / SLACK_DIVISOR).coerceAtLeast(MIN_SLACK)
        val tolerance = (unit.length / TOLERANCE_DIVISOR).coerceAtLeast(MIN_TOLERANCE)
        val shortest = (unit.length - slack).coerceAtLeast(1)
        val longest = minOf(available, unit.length + slack)

        var bestLength = 0
        var bestDistance = Int.MAX_VALUE
        for (length in shortest..longest) {
            val distance = EditDistance.between(heard, from, from + length, unit)
            if (distance < bestDistance) {
                bestDistance = distance
                bestLength = length
            }
        }
        return if (bestDistance <= tolerance) bestLength else null
    }

    private companion object {
        /** How far the heard stretch may differ in length from the expected unit — elongation and
         * dropped phonemes both change length without changing which word was said. */
        const val SLACK_DIVISOR = 3
        const val MIN_SLACK = 2

        /** A third of a unit may be misheard and still count as that unit. Tighter than it sounds:
         * the alternative is not "a different word" but "no movement at all until the server
         * replies", and a wrong guess is capped by the window and corrected within ~2s. */
        const val TOLERANCE_DIVISOR = 3
        const val MIN_TOLERANCE = 1
    }
}
