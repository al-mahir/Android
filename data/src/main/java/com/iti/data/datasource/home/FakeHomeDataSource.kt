package com.iti.data.datasource.home

import com.iti.data.dto.home.ContinueReadingDto
import com.iti.data.dto.home.HomeSummaryDto
import com.iti.data.dto.home.SheikhDto
import com.iti.data.dto.home.StudyCircleDto
import com.iti.data.dto.home.UserDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

/**
 * In-memory stand-in for the Home endpoint until the backend ships.
 *
 * Held as a [MutableStateFlow] rather than a static list so a write ([joinCircle]) re-emits
 * and the UI updates through the same stream the real implementation will use — the screen
 * therefore exercises the production data flow, not a special case.
 */
class FakeHomeDataSource : HomeDataSource {

    private val state = MutableStateFlow(SEED)

    override fun observeHomeSummary(): Flow<HomeSummaryDto> =
        state.asStateFlow().onStart { delay(LOAD_DELAY_MS) }

    override suspend fun joinCircle(circleId: String) {
        delay(JOIN_DELAY_MS)
        state.update { current ->
            current.copy(
                circles = current.circles.map { circle ->
                    if (circle.id == circleId) circle.copy(isJoined = true) else circle
                },
            )
        }
    }

    private companion object {
        /** Long enough for the shimmer state to be visible while developing. */
        const val LOAD_DELAY_MS = 800L
        const val JOIN_DELAY_MS = 400L

        val SEED = HomeSummaryDto(
            user = UserDto(displayName = "Jamal Darwish"),
            continueReading = ContinueReadingDto(
                surahName = "Al-Kahf",
                ayahNumber = 45,
                pageNumber = 298,
            ),
            sheikhs = listOf(
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
            ),
            circles = listOf(
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
            ),
        )
    }
}
