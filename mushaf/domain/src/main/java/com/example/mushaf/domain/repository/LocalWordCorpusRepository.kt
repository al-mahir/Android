package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.local.LocalWordEntry
import com.iti.domain.core.Result

/**
 * Plain, ASR-comparable Qur'an text, entirely local/offline. Deliberately not the same source as
 * page rendering: [com.example.mushaf.domain.model.MushafWord.glyphs] holds Uthmani font
 * ligatures (e.g. a single codepoint for a whole word), not letter-by-letter text a speech
 * transcript can be normalized and compared against.
 */
interface LocalWordCorpusRepository {

    /**
     * Up to [count] words in reading order starting at [cursor] (inclusive), crossing ayah/sūrah
     * boundaries as needed. An ayah whose plain-text word count doesn't line up with the real
     * (Uthmani) word count for that ayah — a known ~6% of the corpus, mostly joined constructs
     * like "يا أيها" written as one Uthmani word but two plain words — is skipped entirely rather
     * than risk mis-addressing a word; callers see a shorter (possibly empty) window instead.
     */
    suspend fun wordsFrom(cursor: RecitationCursor, count: Int): Result<List<LocalWordEntry>>

    /**
     * Every word of one ayah, in reading order.
     *
     * Whole ayahs, not a window: aligning phoneme units to words
     * ([com.example.mushaf.domain.model.recite.local.PhonemeUnitAligner]) needs the complete list,
     * since a tajwīd merge that begins before the window would shift every unit after it.
     *
     * `null` means **the ayah does not exist** — the caller has walked off the end of the sūrah and
     * should roll over to the next one. An *empty list* means it exists but was skipped for the
     * word-count mismatch described on [wordsFrom], where the right move is to carry on to the next
     * ayah. The two look identical to a caller that only sees "no words", and confusing them sends
     * a reading window into the wrong sūrah entirely.
     */
    suspend fun wordsForAyah(sura: Int, aya: Int): Result<List<LocalWordEntry>?>
}
