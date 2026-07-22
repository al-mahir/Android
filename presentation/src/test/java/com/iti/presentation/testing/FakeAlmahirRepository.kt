package com.iti.presentation.testing

import com.iti.domain.model.LegalDocument
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.model.ReadingProgress
import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability
import com.iti.domain.model.StudyCircle
import com.iti.domain.model.Subscription
import com.iti.domain.model.SubscriptionPlan
import com.iti.domain.model.User
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory [AlmahirRepository] shared by the presentation tests.
 *
 * Every failure mode is a constructor flag rather than a subclass, so a test reads as one line
 * of setup, and recorded calls (`joined`, `loggedOut`, `deletedAccount`) let a test assert that
 * the ViewModel actually reached the repository instead of only mutating its own state.
 */
class FakeAlmahirRepository(
    private val user: User = USER,
    private val readingProgress: ReadingProgress? = PROGRESS,
    private val subscription: Subscription = FREE_SUBSCRIPTION,
    private val failSheikhs: Boolean = false,
    private val failSubscription: Boolean = false,
    private val failLogout: Boolean = false,
    private val failDeleteAccount: Boolean = false,
    private val failRestore: Boolean = false,
    private val restoreResult: Boolean = false,
) : AlmahirRepository {

    val joined = mutableListOf<String>()
    var loggedOut = false
        private set
    var deletedAccount = false
        private set
    var restoreCount = 0
        private set

    private val circles = MutableStateFlow(listOf(CIRCLE))

    override fun observeCurrentUser(): Flow<User> = flowOf(user)

    override fun observeReadingProgress(): Flow<ReadingProgress?> = flowOf(readingProgress)

    override fun observeSheikhs(): Flow<List<Sheikh>> =
        if (failSheikhs) flow { throw IllegalStateException("boom") } else flowOf(listOf(SHEIKH))

    override fun observeStudyCircles(): Flow<List<StudyCircle>> = circles

    override fun observeSubscription(): Flow<Subscription> =
        if (failSubscription) flow { throw IllegalStateException("boom") } else flowOf(subscription)

    override fun observeLegalDocument(type: LegalDocumentType): Flow<LegalDocument> = flowOf(
        LegalDocument(
            type = type,
            title = "title-${type.name}",
            body = "# Heading\n\nBody text.",
            updatedAtEpochMillis = null,
        )
    )

    override suspend fun joinStudyCircle(circleId: String) {
        joined += circleId
    }

    override suspend fun restorePurchases(): Boolean {
        restoreCount++
        if (failRestore) throw IllegalStateException("boom")
        return restoreResult
    }

    override suspend fun logout() {
        if (failLogout) throw IllegalStateException("boom")
        loggedOut = true
    }

    override suspend fun deleteAccount() {
        if (failDeleteAccount) throw IllegalStateException("boom")
        deletedAccount = true
    }

    companion object {
        /** 2026-07-14T00:00:00Z. */
        const val JOINED_AT_EPOCH_MILLIS = 1_783_987_200_000L

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
        val PROGRESS = ReadingProgress(surahName = "Al-Kahf", ayahNumber = 45, pageNumber = 298)
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
            title = "دورة",
            hostName = "Omar",
            isJoined = false,
        )
    }
}
