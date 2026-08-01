package com.iti.domain.usecase

import com.iti.domain.core.Result
import com.iti.domain.core.getOrNull
import com.iti.domain.model.CircleDifficulty
import com.iti.domain.model.Sheikh
import com.iti.domain.model.SheikhAvailability
import com.iti.domain.model.StudyCircle
import com.iti.domain.repository.CircleRepository
import com.iti.domain.repository.SheikhRepository
import com.iti.domain.usecase.circle.GetStudyCirclesUseCase
import com.iti.domain.usecase.circle.JoinStudyCircleUseCase
import com.iti.domain.usecase.sheikh.GetSheikhsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SheikhAndCircleOrderingTest {

    @Test
    fun `sheikhs are ordered by reachability then by rating`() = runTest {
        val repository = FakeSheikhRepository(
            sheikhs = listOf(
                sheikh("offline-5", rating = 5.0, availability = SheikhAvailability.OFFLINE),
                sheikh("busy-4", rating = 4.0, availability = SheikhAvailability.IN_SESSION),
                sheikh("free-3", rating = 3.0, availability = SheikhAvailability.AVAILABLE),
                sheikh("free-4", rating = 4.0, availability = SheikhAvailability.AVAILABLE),
            ),
        )

        val ordered = GetSheikhsUseCase(repository)().getOrNull()!!.map { it.id }

        assertEquals(listOf("free-4", "free-3", "busy-4", "offline-5"), ordered)
    }

    @Test
    fun `study circles pass through in the order the repository returns them`() = runTest {
        val repository = FakeCircleRepository(
            circles = listOf(
                circle("joined", isJoined = true),
                circle("open", isJoined = false),
            ),
        )

        val ordered = GetStudyCirclesUseCase(repository)().first().getOrNull()!!.map { it.id }

        assertEquals(listOf("joined", "open"), ordered)
    }

    @Test
    fun `joining rejects a blank id before reaching the repository`() = runTest {
        val repository = FakeCircleRepository()

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

    private fun circle(id: String, isJoined: Boolean) = StudyCircle(
        id = id,
        surahName = id,
        hostId = "host",
        hostName = "host",
        hostInitials = "HO",
        isLive = false,
        difficulty = CircleDifficulty.BEGINNER,
        participantCount = 0,
        maxParticipants = 10,
        currentActivity = "Reading",
        isJoined = isJoined,
    )

    private class FakeSheikhRepository(
        private val sheikhs: List<Sheikh> = emptyList(),
    ) : SheikhRepository {
        override suspend fun getSheikhs(): Result<List<Sheikh>> = Result.Success(sheikhs)

        override suspend fun getSheikhById(id: String): Result<Sheikh?> =
            Result.Success(sheikhs.firstOrNull { it.id == id })

        override suspend fun searchSheikhs(name: String): Result<List<Sheikh>> =
            Result.Success(sheikhs.filter { it.name.contains(name, ignoreCase = true) })
    }

    private class FakeCircleRepository(
        private val circles: List<StudyCircle> = emptyList(),
    ) : CircleRepository {
        val joined = mutableListOf<String>()

        override fun observeStudyCircles(): Flow<Result<List<StudyCircle>>> =
            flowOf(Result.Success(circles))

        override fun observeSheikhCircles(sheikhId: String): Flow<Result<List<StudyCircle>>> =
            flowOf(Result.Success(circles.filter { it.hostName == sheikhId }))

        override suspend fun joinStudyCircle(circleId: String): Result<Unit> {
            joined += circleId
            return Result.Success(Unit)
        }

        override suspend fun cancelJoinCircle(circleId: String): Result<Unit> {
            joined -= circleId
            return Result.Success(Unit)
        }
    }
}
