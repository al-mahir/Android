package com.iti.domain.usecase

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
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.domain.usecase.circle.JoinStudyCircleUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SheikhAndCircleOrderingTest {

    @Test
    fun `sheikhs are ordered by reachability then by rating`() = runTest {
        val repository = FakeRepository(
            sheikhs = listOf(
                sheikh("offline-5", rating = 5.0, availability = SheikhAvailability.OFFLINE),
                sheikh("busy-4", rating = 4.0, availability = SheikhAvailability.IN_SESSION),
                sheikh("free-3", rating = 3.0, availability = SheikhAvailability.AVAILABLE),
                sheikh("free-4", rating = 4.0, availability = SheikhAvailability.AVAILABLE),
            ),
        )

        val ordered = GetSheikhsUseCase(repository)().first().map { it.id }

        assertEquals(listOf("free-4", "free-3", "busy-4", "offline-5"), ordered)
    }

    @Test
    fun `unjoined circles come before joined ones`() = runTest {
        val repository = FakeRepository(
            circles = listOf(
                circle("joined", isJoined = true),
                circle("open", isJoined = false),
            ),
        )

        val ordered = GetStudyCirclesUseCase(repository)().first().map { it.id }

        assertEquals(listOf("open", "joined"), ordered)
    }

    @Test
    fun `joining rejects a blank id before reaching the repository`() = runTest {
        val repository = FakeRepository()

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { JoinStudyCircleUseCase(repository)("  ") }
        }
        assertEquals(emptyList<String>(), repository.joined)
    }

    private fun sheikh(id: String, rating: Double, availability: SheikhAvailability) = Sheikh(
        id = id,
        name = id,
        initials = "XX",
        avatarUrl = null,
        rating = rating,
        availability = availability,
    )

    private fun circle(id: String, isJoined: Boolean) =
        StudyCircle(id = id, title = id, hostName = "host", isJoined = isJoined)

    private class FakeRepository(
        private val sheikhs: List<Sheikh> = emptyList(),
        private val circles: List<StudyCircle> = emptyList(),
    ) : AlmahirRepository {
        val joined = mutableListOf<String>()

        override fun observeCurrentUser(): Flow<User> = flowOf(
            User(
                id = "u",
                displayName = "U",
                initials = "U",
                avatarUrl = null,
                email = "u@example.com",
                joinedAtEpochMillis = 0L,
            )
        )

        override fun observeReadingProgress(): Flow<ReadingProgress?> = flowOf(null)

        override fun observeSheikhs(): Flow<List<Sheikh>> = flowOf(sheikhs)

        override fun observeStudyCircles(): Flow<List<StudyCircle>> = flowOf(circles)

        override fun observeSubscription(): Flow<Subscription> =
            flowOf(Subscription(plan = SubscriptionPlan.NONE, renewsAtEpochMillis = null))

        override fun observeLegalDocument(type: LegalDocumentType): Flow<LegalDocument> =
            emptyFlow()

        override suspend fun joinStudyCircle(circleId: String) {
            joined += circleId
        }

        override suspend fun restorePurchases(): Boolean = false

        override suspend fun logout() = Unit

        override suspend fun deleteAccount() = Unit
    }
}
