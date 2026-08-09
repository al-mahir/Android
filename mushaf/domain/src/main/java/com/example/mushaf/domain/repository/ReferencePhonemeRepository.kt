package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.local.ReferencePhonemeUnit
import com.iti.domain.core.Result

/**
 * How the upcoming text is expected to *sound*, in the same phoneme alphabet the on-device model
 * emits — the reference side of [com.example.mushaf.domain.model.recite.local.PhonemeCursorTracker].
 *
 * Plain Qur'an text can't serve this role. `Muno459/zipformer_p-quran` is a phoneme-level model;
 * comparing its output against script is what made an earlier round of this feature match 0% of
 * everything it heard. The reference is a precomputed table shipped alongside the model, so no
 * phonetizer has to be reimplemented here.
 */
interface ReferencePhonemeRepository {

    /**
     * Pronunciation units covering roughly [wordCount] words starting at [cursor], in reading
     * order. Fewer units than words is normal and expected — see [ReferencePhonemeUnit].
     *
     * Returns an empty list rather than failing when the table isn't downloaded yet or an ayah
     * can't be aligned: local tracking is advisory, and "no prediction" is always a safe answer.
     */
    suspend fun unitsFrom(cursor: RecitationCursor, wordCount: Int): Result<List<ReferencePhonemeUnit>>
}
