package com.example.mushaf.data.repository

import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.data.db.MushafAssetDataSource
import com.example.mushaf.data.db.QuranTextDataSource
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.local.LocalWordEntry
import com.example.mushaf.domain.repository.LocalWordCorpusRepository

/**
 * Builds word-level entries by splitting [QuranTextDataSource]'s plain verse text on whitespace
 * and addressing each token with the same `sura:aya:word` scheme the rest of the recitation
 * pipeline uses ([RecitationCursor.wordId]) — no separate word-level corpus asset needed. Word
 * order within an ayah is inherently sequential, so this only requires knowing *how many* real
 * (non-end-marker) words the layout DB has for that ayah, to detect the rasm/plain-text
 * word-count mismatches noted on [LocalWordCorpusRepository.wordsFrom] and skip them.
 */
class LocalWordCorpusRepositoryImpl(
    private val textDataSource: QuranTextDataSource,
    private val layoutDataSource: MushafAssetDataSource,
) : LocalWordCorpusRepository {

    override suspend fun wordsFrom(cursor: RecitationCursor, count: Int): List<LocalWordEntry> {
        if (count <= 0) return emptyList()
        val result = ArrayList<LocalWordEntry>(count)

        var sura = cursor.sura
        var aya = cursor.aya
        var startWordIndex = cursor.wordIndex
        var scanned = 0

        while (result.size < count && scanned < MAX_AYAHS_SCANNED) {
            scanned++
            val plainText = textDataSource.getVerseText(sura, aya)
            if (plainText == null) {
                if (!advanceToNextSurah(sura)) break
                sura += 1
                aya = 1
                startWordIndex = 0
                continue
            }

            val plainWords = plainText.split(WHITESPACE)
            val realCount = layoutDataSource.wordCountForAyah(sura, aya)
            if (plainWords.size != realCount) {
                Log.d(
                    MushafLog.TAG,
                    "Local corpus: skipping $sura:$aya (plain=${plainWords.size}, layout=$realCount)",
                )
            } else {
                for (index in startWordIndex until plainWords.size) {
                    if (result.size >= count) break
                    result += LocalWordEntry(
                        wordId = "$sura:$aya:${index + 1}",
                        plainText = plainWords[index],
                    )
                }
            }

            aya += 1
            startWordIndex = 0
        }

        return result
    }

    /** No ayah-count table is queried; instead we just check surah 114 has already been passed,
     * since [QuranTextDataSource.getVerseText] returning null for `(sura, 1)` beyond it means
     * the corpus is exhausted. */
    private fun advanceToNextSurah(currentSura: Int): Boolean = currentSura < LAST_SURAH

    private companion object {
        const val LAST_SURAH = 114
        const val MAX_AYAHS_SCANNED = 30
        val WHITESPACE = Regex("\\s+")
    }
}
