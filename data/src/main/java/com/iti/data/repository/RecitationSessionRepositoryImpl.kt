package com.iti.data.repository

import com.iti.data.local.recitation.RecitationSessionDao
import com.iti.data.local.recitation.RecitationSessionEntity
import com.iti.data.local.recitation.StoredMistake
import com.iti.data.local.recitation.StoredPracticeFocus
import com.iti.domain.model.recitation.RecitationSessionSummary
import com.iti.domain.model.recitation.SessionMistake
import com.iti.domain.model.recitation.SessionMistakeCategory
import com.iti.domain.model.recitation.SessionPosition
import com.iti.domain.model.recitation.SessionPracticeFocus
import com.iti.domain.repository.RecitationSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json


class RecitationSessionRepositoryImpl(
    private val dao: RecitationSessionDao,
    private val json: Json = SessionJson,
) : RecitationSessionRepository {

    override fun observeSessions(): Flow<List<RecitationSessionSummary>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeSession(id: String): Flow<RecitationSessionSummary?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun save(summary: RecitationSessionSummary) = dao.upsert(summary.toEntity())

    override suspend fun delete(id: String) = dao.delete(id)

    override suspend fun deleteAll() = dao.deleteAll()

    private fun RecitationSessionSummary.toEntity() = RecitationSessionEntity(
        id = id,
        startedAtEpochMs = startedAtEpochMs,
        durationMs = durationMs,
        startSura = start.sura,
        startAya = start.aya,
        endSura = end.sura,
        endAya = end.aya,
        scoredWordCount = scoredWordCount,
        mistakesJson = json.encodeToString(
            mistakes.map {
                StoredMistake(
                    sura = it.sura,
                    aya = it.aya,
                    wordIndex = it.wordIndex,
                    word = it.word,
                    category = it.category.name,
                    ruleName = it.ruleName,
                    expectedLength = it.expectedLength,
                    actualLength = it.actualLength,
                )
            },
        ),
        practiceFocusJson = json.encodeToString(
            practiceFocus.map {
                StoredPracticeFocus(
                    category = it.category.name,
                    ruleName = it.ruleName,
                    occurrences = it.occurrences,
                )
            },
        ),
    )

    private fun RecitationSessionEntity.toDomain() = RecitationSessionSummary(
        id = id,
        startedAtEpochMs = startedAtEpochMs,
        durationMs = durationMs,
        start = SessionPosition(startSura, startAya),
        end = SessionPosition(endSura, endAya),
        scoredWordCount = scoredWordCount,
        mistakes = decodeMistakes(mistakesJson),
        practiceFocus = decodePracticeFocus(practiceFocusJson),
    )


    private fun decodeMistakes(raw: String): List<SessionMistake> =
        runCatching { json.decodeFromString<List<StoredMistake>>(raw) }
            .getOrDefault(emptyList())
            .map {
                SessionMistake(
                    sura = it.sura,
                    aya = it.aya,
                    wordIndex = it.wordIndex,
                    word = it.word,
                    category = it.category.toCategory(),
                    ruleName = it.ruleName,
                    expectedLength = it.expectedLength,
                    actualLength = it.actualLength,
                )
            }

    private fun decodePracticeFocus(raw: String): List<SessionPracticeFocus> =
        runCatching { json.decodeFromString<List<StoredPracticeFocus>>(raw) }
            .getOrDefault(emptyList())
            .map {
                SessionPracticeFocus(
                    category = it.category.toCategory(),
                    ruleName = it.ruleName,
                    occurrences = it.occurrences,
                )
            }

    private fun String.toCategory(): SessionMistakeCategory =
        SessionMistakeCategory.entries.firstOrNull { it.name == this } ?: SessionMistakeCategory.OTHER
}

internal val SessionJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
