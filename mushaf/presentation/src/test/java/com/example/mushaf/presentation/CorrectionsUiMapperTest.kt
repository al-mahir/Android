package com.example.mushaf.presentation

import com.example.mushaf.domain.model.recite.MistakeCategory
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationMistake
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.RecitationWordStatus
import com.example.mushaf.domain.model.recite.SpeechErrorType
import com.example.mushaf.domain.model.recite.TajweedRuleReference
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.recite.CorrectionsUiMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionsUiMapperTest {

    private fun mistake(
        category: MistakeCategory = MistakeCategory.MEMORIZATION,
        speechErrorType: SpeechErrorType = SpeechErrorType.INSERT,
        rule: TajweedRuleReference? = null,
        expected: Int? = null,
        actual: Int? = null,
    ) = RecitationMistake(
        category = category,
        rawChannel = "normal",
        speechErrorType = speechErrorType,
        uthmaniSpan = null,
        expectedPhonemes = null,
        predictedPhonemes = null,
        expectedLength = expected,
        actualLength = actual,
        rules = listOfNotNull(rule),
        confidence = 0.9f,
    )

    private fun word(
        sura: Int = 1,
        aya: Int = 1,
        index: Int,
        text: String = "w$index",
        status: RecitationWordStatus = RecitationWordStatus.CORRECT,
        trimmed: Boolean = false,
        mistakes: List<RecitationMistake> = emptyList(),
    ) = RecitationWordFeedback(
        position = RecitationCursor(sura, aya, index),
        uthmani = text,
        status = status,
        mistakes = mistakes,
        isTrimmed = trimmed,
    )

    private fun ledger(vararg words: RecitationWordFeedback) = words.associateBy { it.wordId }

    @Test
    fun `an ayah with no mistakes produces no card`() {
        val corrections = CorrectionsUiMapper.toCorrections(
            ledger(word(index = 0), word(index = 1)),
        )

        assertTrue("a clean ayah was listed as a correction", corrections.isEmpty())
    }

    @Test
    fun `hints never produce a card`() {
        
        
        val corrections = CorrectionsUiMapper.toCorrections(
            ledger(word(index = 0, status = RecitationWordStatus.ALMOST, mistakes = listOf(mistake()))),
        )

        assertTrue(corrections.isEmpty())
    }

    @Test
    fun `an unverified word never produces a card`() {
        val corrections = CorrectionsUiMapper.toCorrections(
            ledger(
                word(index = 0, status = RecitationWordStatus.ERROR, trimmed = true, mistakes = listOf(mistake())),
            ),
        )

        assertTrue("an unscored word was reported as a mistake", corrections.isEmpty())
    }

    @Test
    fun `a mistake produces a card carrying the whole ayah for context`() {
        val corrections = CorrectionsUiMapper.toCorrections(
            ledger(
                word(index = 0, text = "ٱلْحَمْدُ", status = RecitationWordStatus.ERROR, mistakes = listOf(mistake())),
                word(index = 1, text = "لِلَّهِ"),
                word(index = 2, text = "رَبِّ"),
            ),
        )

        val card = corrections.single()
        assertEquals("1:1", card.id)
        assertEquals("1:1:1", card.firstMistakeWordId)
        
        assertEquals(3, card.words.size)
        assertEquals(listOf(true, false, false), card.words.map { it.isMistake })
    }

    @Test
    fun `words stay in recitation order regardless of ledger order`() {
        val corrections = CorrectionsUiMapper.toCorrections(
            ledger(
                word(index = 2, text = "third"),
                word(index = 0, text = "first", status = RecitationWordStatus.ERROR, mistakes = listOf(mistake())),
                word(index = 1, text = "second"),
            ),
        )

        assertEquals(listOf("first", "second", "third"), corrections.single().words.map { it.text })
    }

    @Test
    fun `cards are ordered by sura then ayah`() {
        val corrections = CorrectionsUiMapper.toCorrections(
            ledger(
                word(sura = 2, aya = 5, index = 0, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake())),
                word(sura = 1, aya = 7, index = 0, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake())),
                word(sura = 1, aya = 3, index = 0, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake())),
            ),
        )

        assertEquals(listOf("1:3", "1:7", "2:5"), corrections.map { it.id })
    }

    @Test
    fun `the label follows the channel and what the reciter did`() {
        fun labelFor(category: MistakeCategory, type: SpeechErrorType) =
            CorrectionsUiMapper.toCorrections(
                ledger(
                    word(
                        index = 0,
                        status = RecitationWordStatus.ERROR,
                        mistakes = listOf(mistake(category = category, speechErrorType = type)),
                    ),
                ),
            ).single().mistakes.single().findings.single().labelRes

        assertEquals(
            R.string.mushaf_correction_extra_words,
            labelFor(MistakeCategory.MEMORIZATION, SpeechErrorType.INSERT),
        )
        assertEquals(
            R.string.mushaf_correction_missing_words,
            labelFor(MistakeCategory.MEMORIZATION, SpeechErrorType.DELETE),
        )
        assertEquals(
            R.string.mushaf_correction_wrong_words,
            labelFor(MistakeCategory.MEMORIZATION, SpeechErrorType.REPLACE),
        )
        assertEquals(
            R.string.mushaf_correction_tashkeel,
            labelFor(MistakeCategory.TASHKIL, SpeechErrorType.REPLACE),
        )
        assertEquals(
            R.string.mushaf_correction_tajweed,
            labelFor(MistakeCategory.TAJWID, SpeechErrorType.REPLACE),
        )
        
        assertEquals(
            R.string.mushaf_correction_generic,
            labelFor(MistakeCategory.OTHER, SpeechErrorType.UNKNOWN),
        )
    }

    @Test
    fun `a madd finding is explained with both lengths`() {
        val corrections = CorrectionsUiMapper.toCorrections(
            ledger(
                word(
                    index = 0,
                    status = RecitationWordStatus.ERROR,
                    mistakes = listOf(
                        mistake(
                            category = MistakeCategory.TAJWID,
                            rule = TajweedRuleReference("المد الطبيعي", "Normal Madd", 2, "count", "alif"),
                            expected = 2,
                            actual = 3,
                        ),
                    ),
                ),
            ),
        )

        val finding = corrections.single().mistakes.single().findings.single()
        assertEquals("المد الطبيعي", finding.rules.single().nameArabic)
        assertEquals(2, finding.expectedLength)
        assertEquals(3, finding.actualLength)
    }

    @Test
    fun `a finding without lengths gets no invented explanation`() {
        
        
        val corrections = CorrectionsUiMapper.toCorrections(
            ledger(
                word(
                    index = 0,
                    status = RecitationWordStatus.ERROR,
                    mistakes = listOf(
                        mistake(
                            category = MistakeCategory.TAJWID,
                            rule = TajweedRuleReference("الغنة", "Ghonna", null, "match", null),
                        ),
                    ),
                ),
            ),
        )

        val finding = corrections.single().mistakes.single().findings.single()
        assertNull("a length was invented for a rule that reported none", finding.expectedLength)
        assertNull("a length was invented for a rule that reported none", finding.actualLength)
    }

    @Test
    fun `a hint inside a flagged ayah stays context, not an accusation`() {
        val corrections = CorrectionsUiMapper.toCorrections(
            ledger(
                word(index = 0, status = RecitationWordStatus.ERROR, mistakes = listOf(mistake())),
                word(index = 1, status = RecitationWordStatus.ALMOST, mistakes = listOf(mistake())),
            ),
        )

        val words = corrections.single().words
        assertTrue(words[0].isMistake)
        assertFalse("a softened finding was marked as a mistake", words[1].isMistake)
    }

    @Test
    fun `every mistaken word in an ayah gets its own entry`() {
        
        
        val corrections = CorrectionsUiMapper.toCorrections(
            ledger(
                word(
                    index = 0, text = "first", status = RecitationWordStatus.ERROR,
                    mistakes = listOf(mistake(category = MistakeCategory.MEMORIZATION, speechErrorType = SpeechErrorType.INSERT)),
                ),
                word(index = 1, text = "clean"),
                word(
                    index = 2, text = "third", status = RecitationWordStatus.ERROR,
                    mistakes = listOf(mistake(category = MistakeCategory.TASHKIL, speechErrorType = SpeechErrorType.REPLACE)),
                ),
            ),
        )

        val card = corrections.single()
        assertEquals(2, card.mistakes.size)
        assertEquals(listOf("first", "third"), card.mistakes.map { it.word })
        
        assertEquals(R.string.mushaf_correction_extra_words, card.mistakes[0].findings.single().labelRes)
        assertEquals(R.string.mushaf_correction_tashkeel, card.mistakes[1].findings.single().labelRes)
        
        assertEquals(listOf("1:1:1", "1:1:3"), card.mistakes.map { it.wordId })
    }

    @Test
    fun `mistakes stay in recitation order`() {
        val corrections = CorrectionsUiMapper.toCorrections(
            ledger(
                word(index = 4, text = "later", status = RecitationWordStatus.ERROR, mistakes = listOf(mistake())),
                word(index = 1, text = "earlier", status = RecitationWordStatus.ERROR, mistakes = listOf(mistake())),
            ),
        )

        assertEquals(listOf("earlier", "later"), corrections.single().mistakes.map { it.word })
    }
}
