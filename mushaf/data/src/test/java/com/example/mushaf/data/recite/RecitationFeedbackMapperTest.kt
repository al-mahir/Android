package com.example.mushaf.data.recite

import com.example.mushaf.data.recite.remote.ProtocolJson
import com.example.mushaf.data.recite.remote.dto.FeedbackEnvelopeDto
import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.MistakeCategory
import com.example.mushaf.domain.model.recite.MoshafValue
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationMatch
import com.example.mushaf.domain.model.recite.RecitationStrictness
import com.example.mushaf.domain.model.recite.RecitationWordMark
import com.example.mushaf.domain.model.recite.RecitationWordStatus
import com.example.mushaf.domain.model.recite.SpeechErrorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mapping tests driven by the real captures in `docs/API.md`, pasted verbatim into
 * [FakeAiService].
 *
 * The assertions that matter most are not about field copying — they are about the three
 * contract rules the service depends on the client honouring (API.md §5.5), because a client
 * that renders the service's deliberate refusals as assertions undoes its safety property.
 */
class RecitationFeedbackMapperTest {

    private fun chunkFrom(payload: String) = RecitationFeedbackMapper.toChunk(
        ProtocolJson.decodeFromString(FeedbackEnvelopeDto.serializer(), payload),
    )

    // ---- word identity ---------------------------------------------------------------------

    @Test
    fun `word ids convert the service's 0-based index to the layout database's 1-based key`() {
        // The whole per-word highlight depends on this single conversion. Off by one and every
        // mistake lands on the neighbouring word, which reads as the model being wrong.
        val chunk = chunkFrom(FATIHA_FEEDBACK_JSON)

        val words = (chunk.match as RecitationMatch.Matched).words
        assertEquals("1:1:1", words.first().wordId)
        assertEquals(0, words.first().position.wordIndex)
        assertEquals("1:1:4", words.last().wordId)
    }

    @Test
    fun `a cursor exposes the same word id scheme`() {
        assertEquals("2:255:1", RecitationCursor(sura = 2, aya = 255, wordIndex = 0).wordId)
    }

    // ---- the three contract rules ----------------------------------------------------------

    @Test
    fun `a trimmed word is unverified even though the service called it correct`() {
        // The exact case API.md calls out: status "correct", trimmed true. It must not be shown
        // as correct — the word was cut by the chunker and never scored.
        val chunk = chunkFrom(FATIHA_FEEDBACK_JSON)

        val trimmed = chunk.words.single { it.isTrimmed }
        assertEquals(RecitationWordStatus.CORRECT, trimmed.status)
        assertEquals(RecitationWordMark.UNVERIFIED, trimmed.mark)
        assertFalse("an unscored word was counted as a mistake", trimmed.countsAsMistake)
    }

    @Test
    fun `an almost word is a hint and never reaches the mistake list`() {
        val chunk = chunkFrom(almostWordPayload())

        val word = chunk.words.single()
        assertEquals(RecitationWordMark.HINT, word.mark)
        assertFalse(word.countsAsMistake)
        assertTrue("a softened finding leaked into corrections", word.scorableMistakes.isEmpty())
        assertTrue("a hint was counted as a mistake", chunk.mistakeWords.isEmpty())
    }

    @Test
    fun `an ambiguous chunk exposes candidates and no words at all`() {
        val chunk = chunkFrom(AMBIGUOUS_FEEDBACK_JSON)

        val match = chunk.match
        assertTrue("ambiguous mapped to a graded result", match is RecitationMatch.Ambiguous)
        assertEquals(2, (match as RecitationMatch.Ambiguous).candidates.size)
        // Words are unreachable by construction, not merely empty.
        assertTrue(chunk.words.isEmpty())
        assertTrue(chunk.mistakeWords.isEmpty())

        // Candidates carry their text so the reciter is not asked to look up "(27, 30)".
        assertEquals(RecitationCursor(1, 1, 0), match.candidates.first().start)
        assertTrue(match.candidates.first().text!!.isNotBlank())
    }

    @Test
    fun `no_match asserts nothing`() {
        val chunk = chunkFrom(
            """{"type":"feedback","chunk_seq":4,"feedback":{"status":"no_match","words":[],""" +
                """"candidates":[]},"cursor":null}""",
        )

        assertEquals(RecitationMatch.NoMatch, chunk.match)
        assertTrue(chunk.words.isEmpty())
        assertNull(chunk.cursor)
    }

    // ---- findings --------------------------------------------------------------------------

    @Test
    fun `a madd finding keeps the rule and both lengths for its explanation`() {
        val chunk = chunkFrom(MADD_ERROR_FEEDBACK_JSON)

        val word = chunk.words.single()
        assertEquals(RecitationWordMark.MISTAKE, word.mark)
        assertTrue(word.countsAsMistake)

        val mistake = word.scorableMistakes.single()
        assertEquals(MistakeCategory.TAJWID, mistake.category)
        assertEquals(SpeechErrorType.REPLACE, mistake.speechErrorType)
        assertEquals(2, mistake.expectedLength)
        assertEquals(3, mistake.actualLength)
        assertEquals(0.97f, mistake.confidence!!, 0.0001f)
        assertFalse(mistake.isUnscored)
        // "المد الطبيعي: expected 2, you held 3."
        assertEquals("المد الطبيعي", mistake.rules.single().nameArabic)
        assertEquals(2, mistake.rules.single().goldenLength)
    }

    @Test
    fun `uthmani_pos becomes the highlight span`() {
        val chunk = chunkFrom(MADD_ERROR_FEEDBACK_JSON)

        assertEquals(25..26, chunk.words.single().mistakes.single().uthmaniSpan)
    }

    @Test
    fun `a malformed span is dropped rather than guessed at`() {
        // A wrong span highlights the wrong letters, which reads as the model being confused
        // about a word the reciter said correctly.
        val chunk = chunkFrom(errorWithSpan("[7]"))

        assertNull(chunk.words.single().mistakes.single().uthmaniSpan)
    }

    @Test
    fun `error channels map onto the shared taxonomy`() {
        assertEquals(MistakeCategory.MEMORIZATION, categoryOf("normal"))
        assertEquals(MistakeCategory.TASHKIL, categoryOf("tashkeel"))
        assertEquals(MistakeCategory.TAJWID, categoryOf("tajweed"))
        // Ṣifāt are articulation attributes, which live under tajwīd in the shared taxonomy.
        assertEquals(MistakeCategory.TAJWID, categoryOf("sifa"))
        // A channel added server-side must not be silently folded into an existing bucket.
        assertEquals(MistakeCategory.OTHER, categoryOf("something_new"))
    }

    @Test
    fun `a null confidence is unscored, not certain`() {
        // Absence of confidence is not high confidence: the service grades it as a hint at every
        // strictness level, so nothing here may treat null as 1.0.
        val chunk = chunkFrom(errorWithConfidence("null"))

        assertTrue(chunk.words.single().mistakes.single().isUnscored)
        assertNull(chunk.words.single().mistakes.single().confidence)
    }

    // ---- resilience ------------------------------------------------------------------------

    @Test
    fun `an unrecognised word status softens to a hint rather than an accusation`() {
        val chunk = chunkFrom(wordWithStatus("catastrophic"))

        assertEquals(RecitationWordMark.HINT, chunk.words.single().mark)
        assertFalse(chunk.words.single().countsAsMistake)
    }

    @Test
    fun `an unrecognised chunk status declines instead of grading`() {
        val chunk = chunkFrom(
            """{"type":"feedback","chunk_seq":0,"feedback":{"status":"something_new","words":[
               {"sura":1,"aya":1,"word_idx":0,"uthmani":"x","status":"error","errors":[],"trimmed":false}]}}""",
        )

        assertEquals(RecitationMatch.NoMatch, chunk.match)
        assertTrue("words were graded under an unknown status", chunk.words.isEmpty())
    }

    @Test
    fun `forced cut and non-verse segments survive`() {
        val chunk = chunkFrom(FATIHA_FEEDBACK_JSON)
        assertTrue(chunk.forcedCut)

        val withNonVerse = chunkFrom(
            """{"type":"feedback","chunk_seq":0,"feedback":{"status":"ok","words":[],
               "non_verse":["istiaatha","basmalah"]}}""",
        )
        assertEquals(2, withNonVerse.nonVerse.size)
    }

    @Test
    fun `the cursor is carried through as the resume point`() {
        val chunk = chunkFrom(FATIHA_FEEDBACK_JSON)

        assertEquals(RecitationCursor(sura = 1, aya = 1, wordIndex = 3), chunk.cursor)
    }

    @Test
    fun `feedback keyed by word id merges onto a rendered page`() {
        val byId = chunkFrom(FATIHA_FEEDBACK_JSON).wordFeedbackById()

        assertEquals(setOf("1:1:1", "1:1:4"), byId.keys)
    }

    // ---- outbound --------------------------------------------------------------------------

    @Test
    fun `a config with a position becomes a start message carrying it`() {
        val start = RecitationFeedbackMapper.toStartMessage(
            LiveRecitationConfig(
                start = RecitationCursor(sura = 2, aya = 255, wordIndex = 3),
                strictness = RecitationStrictness.STRICT,
                engine = "real",
            ),
        )

        assertEquals(2, start.sura)
        assertEquals(255, start.aya)
        assertEquals(3, start.wordIdx)
        assertEquals("strict", start.strictness)
    }

    @Test
    fun `no graded rules means null, an empty set means no tajweed at all`() {
        // These are different messages: null grades everything, [] grades no tajwīd rule.
        // Collapsing them would silently switch a learner's whole grading mode.
        assertNull(RecitationFeedbackMapper.toStartMessage(configWithRules(null)).rules)
        assertEquals(emptyList<String>(), RecitationFeedbackMapper.toStartMessage(configWithRules(emptySet())).rules)
    }

    @Test
    fun `moshaf values keep their string or integer type`() {
        val start = RecitationFeedbackMapper.toStartMessage(
            LiveRecitationConfig(
                start = null,
                moshaf = mapOf(
                    "madd_monfasel_len" to MoshafValue.Number(4),
                    "recitation_speed" to MoshafValue.Text("murattal"),
                ),
            ),
        )

        // An out-of-range or wrongly-typed value makes the server discard the entire moshaf,
        // which is indistinguishable from the setting being ignored.
        assertEquals("4", start.moshaf!!.getValue("madd_monfasel_len").toString())
        assertEquals("\"murattal\"", start.moshaf!!.getValue("recitation_speed").toString())
    }

    @Test
    fun `an empty moshaf is omitted entirely`() {
        assertNull(RecitationFeedbackMapper.toStartMessage(configWithRules(null)).moshaf)
    }

    // ---- fixtures --------------------------------------------------------------------------

    private fun configWithRules(rules: Set<String>?) =
        LiveRecitationConfig(start = RecitationCursor(1, 1), gradedRules = rules)

    private fun categoryOf(channel: String): MistakeCategory =
        chunkFrom(errorWithChannel(channel)).words.single().mistakes.single().category

    private fun almostWordPayload() = wordWithStatus("almost")

    private fun wordWithStatus(status: String) = """
        {"type":"feedback","chunk_seq":0,"feedback":{"status":"ok","words":[
          {"sura":1,"aya":1,"word_idx":0,"uthmani":"بِسْمِ","status":"$status",
           "errors":[{"error_type":"tajweed","confidence":0.4}],"trimmed":false}]}}
    """

    private fun errorWithChannel(channel: String) = """
        {"type":"feedback","chunk_seq":0,"feedback":{"status":"ok","words":[
          {"sura":1,"aya":1,"word_idx":0,"uthmani":"x","status":"error",
           "errors":[{"error_type":"$channel","confidence":0.9}],"trimmed":false}]}}
    """

    private fun errorWithConfidence(confidence: String) = """
        {"type":"feedback","chunk_seq":0,"feedback":{"status":"ok","words":[
          {"sura":1,"aya":1,"word_idx":0,"uthmani":"x","status":"error",
           "errors":[{"error_type":"normal","confidence":$confidence}],"trimmed":false}]}}
    """

    private fun errorWithSpan(span: String) = """
        {"type":"feedback","chunk_seq":0,"feedback":{"status":"ok","words":[
          {"sura":1,"aya":1,"word_idx":0,"uthmani":"x","status":"error",
           "errors":[{"error_type":"normal","uthmani_pos":$span}],"trimmed":false}]}}
    """
}
