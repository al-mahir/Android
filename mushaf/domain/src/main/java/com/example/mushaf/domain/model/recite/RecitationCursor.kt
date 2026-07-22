package com.example.mushaf.domain.model.recite

/**
 * A position in the muṣḥaf.
 *
 * [sura] and [aya] are 1-based; [wordIndex] is **0-based** within the āyah, matching the service.
 * The layout database keys words 1-based, so [wordId] does the conversion once, here, rather than
 * at every call site — an off-by-one would silently highlight the neighbouring word.
 */
data class RecitationCursor(
    val sura: Int,
    val aya: Int,
    val wordIndex: Int = 0,
) {
    /** Matches `MushafWord.id`, so feedback can be joined to the rendered page by key. */
    val wordId: String get() = "$sura:$aya:${wordIndex + 1}"

    companion object {
        /**
         * The inverse of [wordId]: reads a `MushafWord.id` back into a service cursor.
         *
         * Used to tell the service where the reciter moved when they turn a page or tap an
         * āyah. Returns null for a malformed or non-āyah key rather than guessing — seeking to
         * a fabricated position would make the tracker report mismatches that are not mistakes.
         */
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

/**
 * One possibility when a passage could not be placed uniquely.
 *
 * Carries [text] rather than coordinates alone, because "(27, 30)" is a lookup the reciter would
 * otherwise have to perform themselves.
 */
data class RecitationCandidate(
    val start: RecitationCursor,
    val end: RecitationCursor?,
    val text: String?,
)
