package com.example.mushaf.data.recite

import com.example.mushaf.data.recite.remote.dto.CandidateDto
import com.example.mushaf.data.recite.remote.dto.FeedbackDto
import com.example.mushaf.data.recite.remote.dto.FeedbackEnvelopeDto
import com.example.mushaf.data.recite.remote.dto.FeedbackWordDto
import com.example.mushaf.data.recite.remote.dto.PositionDto
import com.example.mushaf.data.recite.remote.dto.StartSessionDto
import com.example.mushaf.data.recite.remote.dto.TajweedRuleRefDto
import com.example.mushaf.data.recite.remote.dto.WordErrorDto
import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.MistakeCategory
import com.example.mushaf.domain.model.recite.MoshafValue
import com.example.mushaf.domain.model.recite.NonVerseSegment
import com.example.mushaf.domain.model.recite.RecitationCandidate
import com.example.mushaf.domain.model.recite.RecitationChunk
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.RecitationMatch
import com.example.mushaf.domain.model.recite.RecitationMistake
import com.example.mushaf.domain.model.recite.RecitationWordFeedback
import com.example.mushaf.domain.model.recite.RecitationWordStatus
import com.example.mushaf.domain.model.recite.SpeechErrorType
import com.example.mushaf.domain.model.recite.TajweedRuleReference
import kotlinx.serialization.json.JsonPrimitive

/**
 * Translates between the service's wire shapes and the domain.
 *
 * **Unknown values resolve towards silence, never towards accusation.** An unrecognised word
 * status becomes a hint rather than a mistake, and an unrecognised chunk status becomes "no
 * match" rather than a graded result. A server that grows a value should cost this client a
 * little precision, never cause it to correct a reciter who did nothing wrong.
 */
object RecitationFeedbackMapper {

    // ---- outbound ------------------------------------------------------------------------

    fun toStartMessage(config: LiveRecitationConfig): StartSessionDto = StartSessionDto(
        sura = config.start?.sura,
        aya = config.start?.aya,
        wordIdx = config.start?.wordIndex,
        strictness = config.strictness.wireValue,
        engine = config.engine,
        // Null and empty are different messages: null grades every rule, empty grades none.
        rules = config.gradedRules?.toList(),
        moshaf = config.moshaf.takeIf { it.isNotEmpty() }?.mapValues { (_, value) ->
            when (value) {
                is MoshafValue.Text -> JsonPrimitive(value.value)
                is MoshafValue.Number -> JsonPrimitive(value.value)
            }
        },
    )

    // ---- inbound -------------------------------------------------------------------------

    fun toChunk(envelope: FeedbackEnvelopeDto): RecitationChunk = RecitationChunk(
        sequence = envelope.chunkSeq,
        match = envelope.feedback?.toMatch() ?: RecitationMatch.NoMatch,
        cursor = envelope.cursor?.toCursor(),
        forcedCut = envelope.forcedCut,
        nonVerse = envelope.feedback?.nonVerse.orEmpty().map(::toNonVerseSegment),
    )

    private fun FeedbackDto.toMatch(): RecitationMatch = when (status.lowercase()) {
        STATUS_OK -> RecitationMatch.Matched(
            words = words.map { it.toWordFeedback() },
            text = uthmaniText,
            start = span?.toCursor(),
            end = end?.toCursor(),
        )

        STATUS_AMBIGUOUS -> RecitationMatch.Ambiguous(candidates.map { it.toCandidate() })

        // Includes `no_match` and anything unrecognised: assert nothing.
        else -> RecitationMatch.NoMatch
    }

    private fun PositionDto.toCursor() = RecitationCursor(sura, aya, wordIdx)

    private fun CandidateDto.toCandidate() = RecitationCandidate(
        start = RecitationCursor(sura, aya, wordIdx),
        end = end?.toCursor(),
        text = uthmaniText,
    )

    private fun FeedbackWordDto.toWordFeedback() = RecitationWordFeedback(
        position = RecitationCursor(sura, aya, wordIdx),
        uthmani = uthmani,
        status = toWordStatus(status),
        mistakes = errors.map { it.toMistake() },
        isTrimmed = trimmed,
    )

    private fun toWordStatus(raw: String): RecitationWordStatus = when (raw.lowercase()) {
        "correct" -> RecitationWordStatus.CORRECT
        "error" -> RecitationWordStatus.ERROR
        // `almost` and anything unknown. Softening an unrecognised status is the safe direction:
        // a hint costs nothing, a false accusation is the failure this system must not produce.
        else -> RecitationWordStatus.ALMOST
    }

    private fun WordErrorDto.toMistake() = RecitationMistake(
        category = toCategory(errorType),
        rawChannel = errorType,
        speechErrorType = toSpeechErrorType(speechErrorType),
        uthmaniSpan = uthmaniPos.toSpan(),
        expectedPhonemes = expectedPh,
        predictedPhonemes = predictedPh,
        expectedLength = expectedLen,
        actualLength = predictedLen,
        rules = tajweedRules.map { it.toRuleReference() },
        confidence = confidence,
    )

    private fun toCategory(raw: String): MistakeCategory = when (raw.lowercase()) {
        "normal" -> MistakeCategory.MEMORIZATION
        "tashkeel" -> MistakeCategory.TASHKIL
        // Ṣifāt are articulation attributes, which sit under tajwīd in the shared taxonomy.
        "tajweed", "sifa" -> MistakeCategory.TAJWID
        else -> MistakeCategory.OTHER
    }

    private fun toSpeechErrorType(raw: String?): SpeechErrorType = when (raw?.lowercase()) {
        "insert" -> SpeechErrorType.INSERT
        "delete" -> SpeechErrorType.DELETE
        "replace" -> SpeechErrorType.REPLACE
        else -> SpeechErrorType.UNKNOWN
    }

    private fun TajweedRuleRefDto.toRuleReference() = TajweedRuleReference(
        nameArabic = nameAr,
        nameEnglish = nameEn,
        goldenLength = goldenLen,
        correctnessType = correctnessType,
        tag = tag,
    )

    private fun toNonVerseSegment(raw: String): NonVerseSegment = when (raw.lowercase()) {
        "istiaatha" -> NonVerseSegment.ISTIAATHA
        "basmalah" -> NonVerseSegment.BASMALAH
        "sadaka" -> NonVerseSegment.SADAKA
        else -> NonVerseSegment.OTHER
    }

    /**
     * The service sends spans as a two-element array. Anything else is dropped rather than
     * guessed at — a wrong span highlights the wrong letters, which reads as the model being
     * confused about a word the reciter said correctly.
     */
    private fun List<Int>?.toSpan(): IntRange? {
        if (this == null || size != 2) return null
        val (start, end) = this
        return if (end >= start) start..end else null
    }

    private const val STATUS_OK = "ok"
    private const val STATUS_AMBIGUOUS = "ambiguous"
}
