package com.iti.data.datasource.circle

import com.iti.data.dto.StudyCircleDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

/**
 * Temporary fake implementation of [CircleDataSource].
 *
 * Returns realistic mock circles with simulated network delay.
 * When the real Circle backend is ready, simply bind [CircleRemoteDataSource]
 * in the Koin module — no other layer changes needed.
 */
class FakeCircleDataSource : CircleDataSource {

    private val circles = MutableStateFlow(SEED_CIRCLES)

    override fun observeStudyCircles(): Flow<List<StudyCircleDto>> =
        circles.asStateFlow().onStart { delay(DELAY_MS) }

    override suspend fun joinStudyCircle(circleId: String) {
        delay(JOIN_DELAY_MS)
        circles.update { current ->
            current.map { circle ->
                if (circle.id == circleId) circle.copy(isWaitingApproval = true) else circle
            }
        }
    }

    override suspend fun cancelJoinCircle(circleId: String) {
        delay(JOIN_DELAY_MS)
        circles.update { current ->
            current.map { circle ->
                if (circle.id == circleId) circle.copy(isWaitingApproval = false, isJoined = false)
                else circle
            }
        }
    }

    private companion object {
        const val DELAY_MS = 800L
        const val JOIN_DELAY_MS = 400L

        val SEED_CIRCLES = listOf(
            StudyCircleDto(
                id = "circle-yasin",
                surahName = "سورة يس",
                hostId = "sheikh-ahmad",
                hostName = "الشيخ أحمد محمد",
                hostInitials = "أح",
                isLive = true,
                difficulty = "intermediate",
                participantCount = 12,
                maxParticipants = 20,
                currentActivity = "تلاوة",
            ),
            StudyCircleDto(
                id = "circle-kahf",
                surahName = "سورة الكهف",
                hostId = "sheikh-omar",
                hostName = "الشيخ عمر الفاضل",
                hostInitials = "عم",
                isLive = true,
                difficulty = "beginner",
                participantCount = 8,
                maxParticipants = 15,
                currentActivity = "تلاوة",
            ),
            StudyCircleDto(
                id = "circle-baqarah",
                surahName = "سورة البقرة",
                hostId = "sheikh-hassan",
                hostName = "الشيخ حسن خليل",
                hostInitials = "حس",
                isLive = true,
                difficulty = "advanced",
                participantCount = 25,
                maxParticipants = 25,
                currentActivity = "تلاوة",
            ),
            StudyCircleDto(
                id = "circle-juzzamma",
                surahName = "جزء عم",
                hostId = "sheikh-ibrahim",
                hostName = "الشيخ إبراهيم أكرم",
                hostInitials = "إب",
                isLive = true,
                difficulty = "beginner",
                participantCount = 5,
                maxParticipants = 12,
                currentActivity = "تلاوة",
            ),
            StudyCircleDto(
                id = "circle-ayman-baqarah",
                surahName = "سورة البقرة",
                hostId = "sheikh-ayman",
                hostName = "الشيخ أيمن جاد",
                hostInitials = "أي",
                isLive = true,
                difficulty = "intermediate",
                participantCount = 10,
                maxParticipants = 20,
                currentActivity = "مراجعة",
            ),
            StudyCircleDto(
                id = "circle-wahib-yasin",
                surahName = "سورة يس",
                hostId = "sheikh-wahib",
                hostName = "الشيخ أحمد وهيب",
                hostInitials = "وه",
                isLive = false,
                difficulty = "beginner",
                participantCount = 7,
                maxParticipants = 15,
                currentActivity = "تلاوة",
            ),
        )
    }
}
