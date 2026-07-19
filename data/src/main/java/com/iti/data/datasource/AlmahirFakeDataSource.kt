package com.iti.data.datasource

import com.iti.data.dto.ReadingProgressDto
import com.iti.data.dto.SheikhDto
import com.iti.data.dto.StudyCircleDto
import com.iti.data.dto.UserDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update


class AlmahirFakeDataSource : AlmahirDataSource {

    private val circles = MutableStateFlow(SEED_CIRCLES)

    override fun observeCurrentUser(): Flow<UserDto> =
        flow { emit(SEED_USER) }.onStart { delay(USER_DELAY_MS) }

    override fun observeReadingProgress(): Flow<ReadingProgressDto?> =
        flow { emit(SEED_PROGRESS) }.onStart { delay(PROGRESS_DELAY_MS) }

    override fun observeSheikhs(): Flow<List<SheikhDto>> =
        flow { emit(SEED_SHEIKHS) }.onStart { delay(SHEIKHS_DELAY_MS) }

    override fun observeStudyCircles(): Flow<List<StudyCircleDto>> =
        circles.asStateFlow().onStart { delay(CIRCLES_DELAY_MS) }

    override suspend fun joinStudyCircle(circleId: String) {
        delay(JOIN_DELAY_MS)
        circles.update { current ->
            current.map { circle ->
                if (circle.id == circleId) circle.copy(isJoined = true) else circle
            }
        }
    }

    private companion object {
        const val USER_DELAY_MS = 300L
        const val PROGRESS_DELAY_MS = 500L
        const val SHEIKHS_DELAY_MS = 700L
        const val CIRCLES_DELAY_MS = 800L
        const val JOIN_DELAY_MS = 400L

        val SEED_USER = UserDto(
            id = "user-1",
            displayName = "Jamal Darwish",
        )

        val SEED_PROGRESS = ReadingProgressDto(
            surahName = "Al-Kahf",
            ayahNumber = 45,
            pageNumber = 298,
        )

        val SEED_SHEIKHS = listOf(
            SheikhDto(
                id = "sheikh-ahmad",
                name = "الشيخ أحمد",
                rating = 4.9,
                availability = "in_session",
            ),
            SheikhDto(
                id = "sheikh-omar",
                name = "الشيخ عمر",
                rating = 5.0,
                availability = "available",
            ),
            SheikhDto(
                id = "sheikh-yusuf",
                name = "الشيخ يوسف",
                rating = 4.7,
                availability = "offline",
            ),
        )

        val SEED_CIRCLES = listOf(
            StudyCircleDto(
                id = "circle-baqarah",
                title = "دورة مراجعة البقرة",
                hostName = "Omar Al-Fadl",
            ),
            StudyCircleDto(
                id = "circle-kids",
                title = "حلقة الأطفال المبتدئين",
                hostName = "Hassan Khalil",
            ),
        )
    }
}
