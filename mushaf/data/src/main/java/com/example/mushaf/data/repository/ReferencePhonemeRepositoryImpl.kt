package com.example.mushaf.data.repository

import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.data.recite.local.asr.QuranPhonemeReferenceSource
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.local.LocalWordEntry
import com.example.mushaf.domain.model.recite.local.PhonemeUnitAligner
import com.example.mushaf.domain.model.recite.local.ReferencePhonemeUnit
import com.example.mushaf.domain.repository.LocalWordCorpusRepository
import com.example.mushaf.domain.repository.ReferencePhonemeRepository
import com.iti.domain.core.Result
import com.iti.domain.core.getOrNull

/**
 * Builds the reference side of local position tracking: the phonemes the reciter is expected to
 * produce next, addressed by the same `sura:aya:word` ids everything else uses.
 *
 * Alignments are cached per ayah because the window is rebuilt on **every** server chunk (roughly
 * every two seconds) and consecutive windows overlap almost completely - the dynamic program in
 * [PhonemeUnitAligner] is cheap for one ayah but not cheap enough to redo three of them ten times
 * a minute for no new information.
 */
class ReferencePhonemeRepositoryImpl(
    private val words: LocalWordCorpusRepository,
    private val source: QuranPhonemeReferenceSource,
) : ReferencePhonemeRepository {

    private val alignmentCache = object : LinkedHashMap<String, List<ReferencePhonemeUnit>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, List<ReferencePhonemeUnit>>): Boolean =
            size > ALIGNMENT_CACHE_SIZE
    }

    override suspend fun unitsFrom(cursor: RecitationCursor, wordCount: Int): Result<List<ReferencePhonemeUnit>> {
        if (wordCount <= 0) return Result.Success(emptyList())

        val window = ArrayList<ReferencePhonemeUnit>()
        var sura = cursor.sura
        var aya = cursor.aya
        var fromWordIndex = cursor.wordIndex
        var covered = 0
        var scanned = 0

        while (covered < wordCount && scanned < MAX_AYAHS_SCANNED) {
            scanned++
            val ayahWords = words.wordsForAyah(sura, aya).getOrNull()
            if (ayahWords == null) {
                // Walked off the end of the sūrah — roll over so a window opened near the last
                // ayah still fills up. (An ayah that merely got skipped comes back empty, not
                // null, and falls through to the `aya += 1` below.)
                if (sura >= LAST_SURAH) break
                sura += 1
                aya = 1
                fromWordIndex = 0
                continue
            }

            for (unit in alignedUnitsFor(sura, aya, ayahWords)) {
                if (covered >= wordCount) break
                if (wordIndexOf(unit.wordIds.first()) < fromWordIndex) continue
                window += unit
                covered += unit.wordIds.size
            }

            aya += 1
            fromWordIndex = 0
        }

        return Result.Success(window)
    }

    private suspend fun alignedUnitsFor(
        sura: Int,
        aya: Int,
        ayahWords: List<LocalWordEntry>,
    ): List<ReferencePhonemeUnit> {
        val key = "$sura:$aya"
        synchronized(alignmentCache) { alignmentCache[key] }?.let { return it }

        val units = source.unitsFor(sura, aya)
        if (units == null) return emptyList()

        val aligned = PhonemeUnitAligner.align(units, ayahWords)
        if (aligned.isEmpty()) {
            Log.d(
                MushafLog.TAG,
                "Phoneme reference: cannot align $key (${units.size} units, ${ayahWords.size} words)",
            )
        }
        synchronized(alignmentCache) { alignmentCache[key] = aligned }
        return aligned
    }

    /** The 0-based position of a word within its ayah, from its `sura:aya:word` id. */
    private fun wordIndexOf(wordId: String): Int =
        RecitationCursor.fromWordId(wordId)?.wordIndex ?: 0

    private companion object {
        const val LAST_SURAH = 114
        const val MAX_AYAHS_SCANNED = 30
        const val ALIGNMENT_CACHE_SIZE = 16
    }
}
