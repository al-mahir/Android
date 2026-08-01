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
}
