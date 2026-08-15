package com.iti.presentation.testing

import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.domain.model.Bookmark
import com.iti.domain.model.BookmarkType
import com.iti.domain.model.CircleDifficulty
import com.iti.domain.model.LegalDocument
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability
import com.iti.domain.model.StudyCircle
import com.iti.domain.model.Subscription
import com.iti.domain.model.SubscriptionPackage
import com.iti.domain.model.SubscriptionPlan
import com.iti.domain.model.User
import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.repository.CircleRepository
import com.iti.domain.repository.SheikhRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * In-memory [AlmahirRepository] + [SheikhRepository] + [CircleRepository] shared by the
 * presentation tests — mirrors the merged `AlmahirRepositoryImpl` in `:data`, which implements
 * all three on one class.
 *
 * Every failure mode is a constructor flag rather than a subclass, so a test reads as one line
 * of setup, and recorded calls (`joined`, `loggedOut`, `deletedAccount`) let a test assert that
 * the ViewModel actually reached the repository instead of only mutating its own state. Failures
 * are returned as [Result.Error], never thrown — matching the real repository's contract.
 */
class FakeAlmahirRepository(
    private val user: User = USER,
    private val subscription: Subscription = FREE_SUBSCRIPTION,
    private val packages: List<SubscriptionPackage> = emptyList(),
    private val sheikhs: List<Sheikh> = listOf(SHEIKH),
    initialCircles: List<StudyCircle> = listOf(CIRCLE),
    private val failSheikhs: Boolean = false,
    private val failSubscription: Boolean = false,
    private val failLogout: Boolean = false,
    private val failDeleteAccount: Boolean = false,
    private val failCancellation: Boolean = false,
) : AlmahirRepository, SheikhRepository, CircleRepository {

    val joined = mutableListOf<String>()
    var loggedOut = false
        private set
    var deletedAccount = false
        private set
    val cancellationRequests = mutableListOf<String>()

    private val circles = MutableStateFlow(initialCircles)
    private val bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())

    // ── AlmahirRepository ────────────────────────────────────────────────

    override fun observeCurrentUser(): Flow<Result<User>> = flowOf(Result.Success(user))

    override fun observeSubscription(): Flow<Result<Subscription>> = flowOf(
        if (failSubscription) Result.Error(BOOM) else Result.Success(subscription)
    )

    override fun observeSubscriptionPackages(): Flow<Result<List<SubscriptionPackage>>> =
        flowOf(Result.Success(packages))

    override suspend fun startFreeTrial(): Result<Subscription> = Result.Success(subscription)

    override suspend fun selectSubscriptionPackage(packageId: String): Result<Subscription> =
        Result.Success(subscription)

    override fun observeLegalDocument(type: LegalDocumentType): Flow<Result<LegalDocument>> = flowOf(
        Result.Success(
            LegalDocument(
                type = type,
                title = "title-${type.name}",
                body = "# Heading\n\nBody text.",
                updatedAtEpochMillis = null,
            )
        )
    )

    override suspend fun requestSubscriptionCancellation(message: String): Result<Unit> {
        cancellationRequests += message
        return if (failCancellation) Result.Error(BOOM) else Result.Success(Unit)
    }

    override suspend fun logout(): Result<Unit> {
        if (failLogout) return Result.Error(BOOM)
        loggedOut = true
        return Result.Success(Unit)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        if (failDeleteAccount) return Result.Error(BOOM)
        deletedAccount = true
        return Result.Success(Unit)
    }

    override fun observeBookmarks(type: BookmarkType): Flow<Result<List<Bookmark>>> =
        bookmarks.map { list -> Result.Success(list.filter { it.type == type }) }

    override fun observeAllBookmarks(): Flow<Result<List<Bookmark>>> =
        bookmarks.map { Result.Success(it) }

    override suspend fun getBookmarks(type: BookmarkType): Result<List<Bookmark>> =
        Result.Success(bookmarks.value.filter { it.type == type })

    override suspend fun getBookmark(id: String): Result<Bookmark?> =
        Result.Success(bookmarks.value.firstOrNull { it.id == id })

    override suspend fun addBookmark(bookmark: Bookmark): Result<Unit> {
        bookmarks.value = bookmarks.value.filterNot { it.id == bookmark.id } + bookmark
        return Result.Success(Unit)
    }

    override suspend fun removeBookmark(id: String): Result<Unit> {
        bookmarks.value = bookmarks.value.filterNot { it.id == id }
        return Result.Success(Unit)
    }

    override fun observeMeetingStatuses(userId: String): Flow<Result<List<com.iti.domain.model.MeetingStatus>>> =
        flowOf(Result.Success(emptyList()))

    override suspend fun saveMeetingStatus(status: com.iti.domain.model.MeetingStatus): Result<Unit> =
        Result.Success(Unit)

    override suspend fun getSheikhs(): Result<List<Sheikh>> =
        if (failSheikhs) Result.Error(BOOM) else Result.Success(sheikhs)

    override suspend fun getSheikhById(id: String): Result<Sheikh?> =
        Result.Success(sheikhs.firstOrNull { it.id == id })

    override suspend fun searchSheikhs(name: String): Result<List<Sheikh>> =
        Result.Success(sheikhs.filter { it.name.contains(name, ignoreCase = true) })

    // ── CircleRepository ──────────────────────────────────────────────────

    override fun observeStudyCircles(): Flow<Result<List<StudyCircle>>> = circles.map { Result.Success(it) }

    override fun observeSheikhCircles(sheikhId: String): Flow<Result<List<StudyCircle>>> =
        circles.map { all -> Result.Success(all.filter { it.hostName == sheikhId }) }

    override suspend fun joinStudyCircle(circleId: String): Result<Unit> {
        joined += circleId
        return Result.Success(Unit)
    }

    override suspend fun cancelJoinCircle(circleId: String): Result<Unit> {
        joined -= circleId
        return Result.Success(Unit)
    }

    companion object {
        /** 2026-07-14T00:00:00Z. */
        const val JOINED_AT_EPOCH_MILLIS = 1_783_987_200_000L

        private val BOOM = DomainError.Unknown(IllegalStateException("boom"))

        val USER = User(
            id = "user-1",
            displayName = "Jamal Darwish",
            initials = "JD",
            avatarUrl = null,
            email = "jamal@example.com",
            joinedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
        )
        val FREE_SUBSCRIPTION = Subscription(
            plan = SubscriptionPlan.NONE,
            renewsAtEpochMillis = null,
        )
        val PREMIUM_SUBSCRIPTION = Subscription(
            plan = SubscriptionPlan.PREMIUM,
            renewsAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
        )
        val SHEIKH = Sheikh(
            id = "sheikh-1",
            name = "الشيخ أحمد",
            initials = "أح",
            avatarUrl = null,
            rating = 4.9,
            availability = SheikhAvailability.IN_SESSION,
        )
        val CIRCLE = StudyCircle(
            id = "circle-1",
            surahName = "دورة",
            hostId = "sheikh-1",
            hostName = "Omar",
            hostInitials = "عم",
            isLive = true,
            difficulty = CircleDifficulty.BEGINNER,
            participantCount = 8,
            maxParticipants = 15,
            currentActivity = "Reading",
            isJoined = false,
        )
    }
}
