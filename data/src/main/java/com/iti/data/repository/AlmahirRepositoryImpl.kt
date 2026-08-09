package com.iti.data.repository

import com.iti.data.core.error.toDomainError
import com.iti.data.datasource.AlmahirDataSource
import com.iti.data.datasource.AlmahirLocalDataSource
import com.iti.data.datasource.sheikh.SheikhDataSource
import com.iti.data.local.recitation.RecitationSessionDao
import com.iti.data.local.recitation.RecitationSessionEntity
import com.iti.data.local.recitation.StoredMistake
import com.iti.data.local.recitation.StoredPracticeFocus
import com.iti.data.mapper.toDomain
import com.iti.data.mapper.toSlug
import com.iti.domain.core.Result
import com.iti.domain.core.asResult
import com.iti.domain.core.resultOf
import com.iti.domain.model.LegalDocument
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.model.Sheikh
import com.iti.domain.model.Subscription
import com.iti.domain.model.SubscriptionPackage
import com.iti.domain.model.User
import com.iti.domain.model.recitation.RecitationSessionSummary
import com.iti.domain.model.recitation.SessionMistake
import com.iti.domain.model.recitation.SessionMistakeCategory
import com.iti.domain.model.recitation.SessionPosition
import com.iti.domain.model.recitation.SessionPracticeFocus
import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.repository.RecitationSessionRepository
import com.iti.domain.repository.SheikhRepository
import com.iti.domain.settings.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json


class AlmahirRepositoryImpl(
    private val dataSource: AlmahirDataSource,
    private val sheikhDataSource: SheikhDataSource,
    private val dao: RecitationSessionDao,
    private val meetingStatusDao: com.iti.data.local.meeting.MeetingStatusDao,
    private val localDataSource: AlmahirLocalDataSource,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val json: Json = SessionJson,
) : AlmahirRepository, SheikhRepository, RecitationSessionRepository {

    // ── AlmahirRepository ────────────────────────────────────────────────

    override fun observeCurrentUser(): Flow<Result<User>> =
        dataSource.observeCurrentUser().map { dto -> dto.toDomain() }.asResult()

    override fun observeSubscription(): Flow<Result<Subscription>> =
        dataSource.observeSubscription().map { dto -> dto.toDomain() }.asResult()

    override fun observeSubscriptionPackages(): Flow<Result<List<SubscriptionPackage>>> =
        dataSource.observeSubscriptionPackages()
            .map { dtos -> dtos.map { it.toDomain() } }
            .asResult()

    override suspend fun startFreeTrial(): Result<Subscription> =
        resultOf { dataSource.startFreeTrial().toDomain() }

    override suspend fun selectSubscriptionPackage(packageId: String): Result<Subscription> =
        resultOf { dataSource.selectSubscriptionPackage(packageId).toDomain() }

    override fun observeLegalDocument(type: LegalDocumentType): Flow<Result<LegalDocument>> =
        dataSource.observeLegalDocument(type.toSlug()).map { dto -> dto.toDomain() }.asResult()

    override suspend fun requestSubscriptionCancellation(message: String): Result<Unit> =
        resultOf { dataSource.requestSubscriptionCancellation(message) }

    override suspend fun logout(): Result<Unit> = resultOf {
        dataSource.logout()
        appPreferencesRepository.clearUser()
    }

    override suspend fun deleteAccount(): Result<Unit> = resultOf {
        dataSource.deleteAccount()
        appPreferencesRepository.clearUser()
    }

    override fun observeBookmarks(type: com.iti.domain.model.BookmarkType): Flow<Result<List<com.iti.domain.model.Bookmark>>> =
        localDataSource?.observeBookmarksByType(type.name)?.map { entities ->
            entities.map { it.toDomain() }
        }?.asResult() ?: kotlinx.coroutines.flow.flowOf(Result.Success(emptyList()))

    override fun observeAllBookmarks(): Flow<Result<List<com.iti.domain.model.Bookmark>>> =
        localDataSource?.observeAllBookmarks()?.map { entities ->
            entities.map { it.toDomain() }
        }?.asResult() ?: kotlinx.coroutines.flow.flowOf(Result.Success(emptyList()))

    override suspend fun getBookmarks(type: com.iti.domain.model.BookmarkType): Result<List<com.iti.domain.model.Bookmark>> = resultOf {
        localDataSource?.getBookmarksByType(type.name)?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getBookmark(id: String): Result<com.iti.domain.model.Bookmark?> = resultOf {
        localDataSource?.getBookmarkById(id)?.toDomain()
    }

    override suspend fun addBookmark(bookmark: com.iti.domain.model.Bookmark): Result<Unit> =
        resultOf { localDataSource?.upsertBookmark(bookmark.toEntity()) }

    override suspend fun removeBookmark(id: String): Result<Unit> =
        resultOf { localDataSource?.deleteBookmark(id) }

    override fun observeMeetingStatuses(userId: String): Flow<Result<List<com.iti.domain.model.MeetingStatus>>> =
        meetingStatusDao.observeMeetingStatuses(userId).map { entities ->
            entities.map { it.toDomain() }
        }.asResult()

    override suspend fun saveMeetingStatus(status: com.iti.domain.model.MeetingStatus): Result<Unit> =
        resultOf { meetingStatusDao.insert(status.toEntity()) }

    // ── SheikhRepository ──────────────────────────────────────────────────

    override suspend fun getSheikhs(): Result<List<Sheikh>> = resultOf(mapError = { it.toDomainError() }) {
        sheikhDataSource.getSheikhs().map { it.toDomain() }
    }

    override suspend fun getSheikhById(id: String): Result<Sheikh?> = resultOf(mapError = { it.toDomainError() }) {
        sheikhDataSource.getSheikhById(id)?.toDomain()
    }

    override suspend fun searchSheikhs(name: String): Result<List<Sheikh>> = resultOf(mapError = { it.toDomainError() }) {
        sheikhDataSource.searchSheikhs(name).map { it.toDomain() }
    }

    // ── RecitationSessionRepository ──────────────────────────────────────

    override fun observeSessions(): Flow<Result<List<RecitationSessionSummary>>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }.asResult()

    override fun observeSession(id: String): Flow<Result<RecitationSessionSummary?>> =
        dao.observeById(id).map { it?.toDomain() }.asResult()

    override suspend fun save(summary: RecitationSessionSummary): Result<Unit> =
        resultOf { dao.upsert(summary.toEntity()) }

    override suspend fun delete(id: String): Result<Unit> = resultOf { dao.delete(id) }

    override suspend fun deleteAll(): Result<Unit> = resultOf { dao.deleteAll() }

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

    private fun com.iti.domain.model.Bookmark.toEntity() = com.iti.data.local.bookmark.BookmarkEntity(
        id = id,
        type = type.name,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        pageNumber = pageNumber,
        sheikhId = sheikhId,
        note = note,
        createdAtEpochMillis = createdAtEpochMillis
    )

    private fun com.iti.data.local.bookmark.BookmarkEntity.toDomain() = com.iti.domain.model.Bookmark(
        id = id,
        type = com.iti.domain.model.BookmarkType.valueOf(type),
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        pageNumber = pageNumber,
        sheikhId = sheikhId,
        note = note,
        createdAtEpochMillis = createdAtEpochMillis
    )

    private fun com.iti.domain.model.MeetingStatus.toEntity() = com.iti.data.local.meeting.MeetingStatusEntity(
        id = id,
        userId = userId,
        meetingTime = meetingTime,
        status = status
    )

    private fun com.iti.data.local.meeting.MeetingStatusEntity.toDomain() = com.iti.domain.model.MeetingStatus(
        id = id,
        userId = userId,
        meetingTime = meetingTime,
        status = status
    )
}

internal val SessionJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
