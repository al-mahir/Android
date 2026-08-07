package com.iti.data

import com.iti.data.datasource.AlmahirDataSource
import com.iti.data.datasource.circle.CircleDataSource
import com.iti.data.datasource.sheikh.SheikhDataSource
import com.iti.data.dto.LegalDocumentDto
import com.iti.data.dto.StudyCircleDto
import com.iti.data.dto.SubscriptionDto
import com.iti.data.dto.SubscriptionPackageDto
import com.iti.data.dto.UserDto
import com.iti.data.dto.sheikh.SheikhApiDto
import com.iti.data.local.recitation.RecitationSessionDao
import com.iti.data.local.recitation.RecitationSessionEntity
import com.iti.data.repository.AlmahirRepositoryImpl
import com.iti.domain.core.getOrNull
import com.iti.domain.model.recitation.RecitationSessionSummary
import com.iti.domain.model.recitation.SessionMistake
import com.iti.domain.model.recitation.SessionMistakeCategory
import com.iti.domain.model.recitation.SessionPosition
import com.iti.domain.model.recitation.SessionPracticeFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Storage round trip for a session record.
 *
 * The lists are held as JSON in one row, so these tests cover the part a schema would otherwise
 * enforce: that everything survives the trip, and that a row this build cannot fully understand
 * degrades instead of taking the history screen down with it.
 */
class RecitationSessionRepositoryTest {

    private class InMemoryDao : RecitationSessionDao {
        val rows = MutableStateFlow<List<RecitationSessionEntity>>(emptyList())

        override fun observeAll(): Flow<List<RecitationSessionEntity>> = rows

        override fun observeById(id: String): Flow<RecitationSessionEntity?> =
            rows.map { all -> all.firstOrNull { it.id == id } }

        override suspend fun upsert(session: RecitationSessionEntity) {
            rows.value = rows.value.filterNot { it.id == session.id } + session
        }

        override suspend fun delete(id: String) {
            rows.value = rows.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            rows.value = emptyList()
        }
    }

    private class StubAlmahirDataSource : AlmahirDataSource {
        override fun observeCurrentUser(): Flow<UserDto> = MutableStateFlow(UserDto(id = "u", displayName = "u"))
        override fun observeSubscription(): Flow<SubscriptionDto> = MutableStateFlow(SubscriptionDto(plan = "none"))
        override fun observeSubscriptionPackages(): Flow<List<SubscriptionPackageDto>> =
            MutableStateFlow(emptyList())
        override suspend fun startFreeTrial(): SubscriptionDto = SubscriptionDto(plan = "none")
        override suspend fun selectSubscriptionPackage(packageId: String): SubscriptionDto =
            SubscriptionDto(plan = "none")
        override fun observeLegalDocument(documentType: String): Flow<LegalDocumentDto> =
            MutableStateFlow(LegalDocumentDto(type = documentType, title = "", body = ""))
        override suspend fun requestSubscriptionCancellation(message: String) = Unit
        override suspend fun logout() = Unit
        override suspend fun deleteAccount() = Unit
    }

    private class StubSheikhDataSource : SheikhDataSource {
        override suspend fun getSheikhs(): List<SheikhApiDto> = emptyList()
        override suspend fun getSheikhById(id: String): SheikhApiDto? = null
        override suspend fun searchSheikhs(name: String): List<SheikhApiDto> = emptyList()
    }

    private class StubCircleDataSource : CircleDataSource {
        override fun observeStudyCircles(): Flow<List<StudyCircleDto>> = MutableStateFlow(emptyList())
        override suspend fun joinStudyCircle(circleId: String) = Unit
        override suspend fun cancelJoinCircle(circleId: String) = Unit
    }

    private val dao = InMemoryDao()
    private val repository = AlmahirRepositoryImpl(
        dataSource = StubAlmahirDataSource(),
        sheikhDataSource = StubSheikhDataSource(),
        circleDataSource = StubCircleDataSource(),
        dao = dao,
    )

    private fun summary(id: String = "s1") = RecitationSessionSummary(
        id = id,
        startedAtEpochMs = 1_700_000_000_000L,
        durationMs = 125_000L,
        start = SessionPosition(1, 1),
        end = SessionPosition(1, 7),
        scoredWordCount = 29,
        mistakes = listOf(
            SessionMistake(
                sura = 1,
                aya = 3,
                wordIndex = 2,
                word = "ٱلرَّحْمَٰنِ",
                category = SessionMistakeCategory.TAJWID,
                ruleName = "المد الطبيعي",
                expectedLength = 2,
                actualLength = 3,
            ),
            SessionMistake(
                sura = 1,
                aya = 7,
                wordIndex = 0,
                word = "صِرَٰطَ",
                category = SessionMistakeCategory.MEMORIZATION,
            ),
        ),
        practiceFocus = listOf(
            SessionPracticeFocus(SessionMistakeCategory.TAJWID, "المد الطبيعي", 4),
        ),
    )

    @Test
    fun `a saved session comes back intact`() = runTest {
        repository.save(summary())

        val stored = repository.observeSessions().first().getOrNull()!!.single()
        assertEquals(summary(), stored)
    }

    @Test
    fun `Arabic text and rule names survive the round trip`() = runTest {
        repository.save(summary())

        val mistake = repository.observeSession("s1").first().getOrNull()!!.mistakes.first()
        assertEquals("ٱلرَّحْمَٰنِ", mistake.word)
        assertEquals("المد الطبيعي", mistake.ruleName)
        assertEquals(2, mistake.expectedLength)
        assertEquals(3, mistake.actualLength)
    }

    @Test
    fun `derived figures are recomputed from what was stored`() = runTest {
        repository.save(summary())

        val stored = repository.observeSession("s1").first().getOrNull()!!
        assertEquals(2, stored.mistakeCount)
        assertEquals((29 - 2) / 29f, stored.accuracy!!, 0.0001f)
        assertEquals(1, stored.mistakesByCategory[SessionMistakeCategory.TAJWID])
    }

    @Test
    fun `a session that graded nothing is kept, not discarded`() = runTest {
        // "I recited and nothing was checked" is information. Dropping it would make a broken
        // connection indistinguishable from a session that never happened.
        repository.save(summary().copy(scoredWordCount = 0, mistakes = emptyList()))

        val stored = repository.observeSessions().first().getOrNull()!!.single()
        assertTrue(stored.gradedNothing)
        assertNull(stored.accuracy)
    }

    @Test
    fun `a category this build does not know degrades instead of throwing`() = runTest {
        // A row written by a newer build must not take down the history screen.
        dao.upsert(
            RecitationSessionEntity(
                id = "future",
                startedAtEpochMs = 0,
                durationMs = 0,
                startSura = 1, startAya = 1, endSura = 1, endAya = 1,
                scoredWordCount = 1,
                mistakesJson = """[{"sura":1,"aya":1,"wordIndex":0,"word":"x","category":"QIRAAT_STYLE"}]""",
                practiceFocusJson = "[]",
            ),
        )

        val stored = repository.observeSession("future").first().getOrNull()!!
        assertEquals(SessionMistakeCategory.OTHER, stored.mistakes.single().category)
    }

    @Test
    fun `a corrupt row loses its detail but not the session`() = runTest {
        // Losing one session's detail is recoverable; losing access to all of them is not.
        dao.upsert(
            RecitationSessionEntity(
                id = "corrupt",
                startedAtEpochMs = 0,
                durationMs = 0,
                startSura = 1, startAya = 1, endSura = 1, endAya = 1,
                scoredWordCount = 5,
                mistakesJson = "not json at all",
                practiceFocusJson = "{{{",
            ),
        )

        val stored = repository.observeSession("corrupt").first().getOrNull()!!
        assertTrue(stored.mistakes.isEmpty())
        assertTrue(stored.practiceFocus.isEmpty())
        assertEquals(5, stored.scoredWordCount)
    }

    @Test
    fun `saving the same id twice replaces rather than duplicates`() = runTest {
        repository.save(summary())
        repository.save(summary().copy(scoredWordCount = 99))

        val all = repository.observeSessions().first().getOrNull()!!
        assertEquals(1, all.size)
        assertEquals(99, all.single().scoredWordCount)
    }

    @Test
    fun `deleting removes only that session`() = runTest {
        repository.save(summary("a"))
        repository.save(summary("b"))

        repository.delete("a")

        assertEquals(listOf("b"), repository.observeSessions().first().getOrNull()!!.map { it.id })
        assertNull(repository.observeSession("a").first().getOrNull())
    }
}
