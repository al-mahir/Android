package com.iti.data.datasource

import com.iti.data.dto.LegalDocumentDto
import com.iti.data.dto.ReadingProgressDto
import com.iti.data.dto.SheikhDto
import com.iti.data.dto.StudyCircleDto
import com.iti.data.dto.SubscriptionDto
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
    private val subscription = MutableStateFlow(SEED_SUBSCRIPTION)

    override fun observeCurrentUser(): Flow<UserDto> =
        flow { emit(SEED_USER) }.onStart { delay(USER_DELAY_MS) }

    override fun observeReadingProgress(): Flow<ReadingProgressDto?> =
        flow { emit(SEED_PROGRESS) }.onStart { delay(PROGRESS_DELAY_MS) }

    override fun observeSheikhs(): Flow<List<SheikhDto>> =
        flow { emit(SEED_SHEIKHS) }.onStart { delay(SHEIKHS_DELAY_MS) }

    override fun observeStudyCircles(): Flow<List<StudyCircleDto>> =
        circles.asStateFlow().onStart { delay(CIRCLES_DELAY_MS) }

    override fun observeSubscription(): Flow<SubscriptionDto> =
        subscription.asStateFlow().onStart { delay(SUBSCRIPTION_DELAY_MS) }

    override fun observeLegalDocument(documentType: String): Flow<LegalDocumentDto> =
        flow {
            val document = SEED_DOCUMENTS[documentType]
                ?: error("Unknown legal document type: $documentType")
            emit(document)
        }.onStart { delay(DOCUMENT_DELAY_MS) }

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

    override suspend fun restorePurchases(): Boolean {
        delay(RESTORE_DELAY_MS)
        return false
    }

    override suspend fun logout() {
        delay(LOGOUT_DELAY_MS)
    }

    override suspend fun deleteAccount() {
        delay(DELETE_ACCOUNT_DELAY_MS)
    }

    private companion object {
        const val USER_DELAY_MS = 300L
        const val PROGRESS_DELAY_MS = 500L
        const val SHEIKHS_DELAY_MS = 700L
        const val CIRCLES_DELAY_MS = 800L
        const val SUBSCRIPTION_DELAY_MS = 400L
        const val DOCUMENT_DELAY_MS = 600L
        const val JOIN_DELAY_MS = 400L
        const val RESTORE_DELAY_MS = 900L
        const val LOGOUT_DELAY_MS = 500L
        const val DELETE_ACCOUNT_DELAY_MS = 1_200L

        const val JOINED_AT_EPOCH_MILLIS = 1_783_987_200_000L

        val SEED_USER = UserDto(
            id = "user-1",
            displayName = "yassenRamadan1",
            email = "yassenr.hassan@gmail.com",
            joinedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
        )

        val SEED_SUBSCRIPTION = SubscriptionDto(plan = "none")

        val SEED_PROGRESS = ReadingProgressDto(
            surahName = "Al-Kahf",
            ayahNumber = 45,
            pageNumber = 298,
            juzNumber = 15,
            surahTotalAyahs = 110,
            surahReadAyahs = 45,
        )

        val SEED_SHEIKHS = listOf(
            SheikhDto(
                id = "sheikh-ahmad",
                name = "أحمد محمد موسى محمد",
                rating = 5.0,
                reviewCount = 128,
                availability = "in_session",
                specialization = "حفظ القرآن الكريم",
                bio = "شيخ متخصص في تعليم حفظ القرآن الكريم بأسانيد عالية، خبرة تزيد عن ١٥ عاماً.",
                activeCircleCount = 2,
                totalStudents = 340,
            ),
            SheikhDto(
                id = "sheikh-wahib",
                name = "أحمد وهيب إبراهيم علي",
                rating = 4.9,
                reviewCount = 95,
                availability = "in_session",
                specialization = "تجويد القرآن",
                bio = "معلم تجويد معتمد من الأزهر الشريف، يدرّس عبر الإنترنت منذ ٢٠١٥.",
                activeCircleCount = 1,
                totalStudents = 215,
            ),
            SheikhDto(
                id = "sheikh-ayman",
                name = "أيمن جاد الحسيني",
                rating = 4.9,
                reviewCount = 74,
                availability = "available",
                specialization = "مراجعة وتثبيت الحفظ",
                bio = "متخصص في برامج مراجعة الحفظ وتثبيته مع الفهم والتدبر.",
                activeCircleCount = 3,
                totalStudents = 180,
            ),
            SheikhDto(
                id = "sheikh-ibrahim",
                name = "إبراهيم أكرم الدسوقي",
                rating = 4.8,
                reviewCount = 61,
                availability = "available",
                specialization = "تعليم الأطفال",
                bio = "خبير في أساليب تعليم القرآن للأطفال باستخدام الطرق التفاعلية الحديثة.",
                activeCircleCount = 2,
                totalStudents = 290,
            ),
            SheikhDto(
                id = "sheikh-omar",
                name = "الشيخ عمر الفاضل",
                rating = 5.0,
                reviewCount = 203,
                availability = "available",
                specialization = "حفظ وتجويد",
                bio = "شيخ محقق في علوم القرآن، حاصل على إجازة بالسند المتصل.",
                activeCircleCount = 1,
                totalStudents = 510,
            ),
            SheikhDto(
                id = "sheikh-hassan",
                name = "الشيخ حسن خليل",
                rating = 4.7,
                reviewCount = 49,
                availability = "offline",
                specialization = "حفظ للمبتدئين",
                bio = "يتميز بأسلوب المبسط لحفظ القرآن للمبتدئين والأطفال.",
                activeCircleCount = 0,
                totalStudents = 155,
            ),
        )

        val SEED_CIRCLES = listOf(
            StudyCircleDto(
                id = "circle-yasin",
                surahName = "Surah Yasin",
                hostId = "sheikh-ahmad",
                hostName = "Sheikh Ahmad",
                hostInitials = "أح",
                isLive = true,
                difficulty = "intermediate",
                participantCount = 12,
                maxParticipants = 20,
                currentActivity = "Reading",
            ),
            StudyCircleDto(
                id = "circle-kahf",
                surahName = "Surah Al-Kahf",
                hostId = "sheikh-omar",
                hostName = "Sheikh Omar",
                hostInitials = "عم",
                isLive = true,
                difficulty = "beginner",
                participantCount = 8,
                maxParticipants = 15,
                currentActivity = "Reading",
            ),
            StudyCircleDto(
                id = "circle-baqarah",
                surahName = "Surah Al-Baqarah",
                hostId = "sheikh-hassan",
                hostName = "Sheikh Hassan",
                hostInitials = "حس",
                isLive = true,
                difficulty = "advanced",
                participantCount = 25,
                maxParticipants = 25,
                currentActivity = "Reading",
            ),
            StudyCircleDto(
                id = "circle-juzzamma",
                surahName = "Juz Amma",
                hostId = "sheikh-ibrahim",
                hostName = "Sheikh Ibrahim",
                hostInitials = "إب",
                isLive = true,
                difficulty = "beginner",
                participantCount = 5,
                maxParticipants = 12,
                currentActivity = "Reading",
            ),
            StudyCircleDto(
                id = "circle-ayman-baqarah",
                surahName = "Surah Al-Baqarah",
                hostId = "sheikh-ayman",
                hostName = "Sheikh Ayman",
                hostInitials = "أي",
                isLive = true,
                difficulty = "intermediate",
                participantCount = 10,
                maxParticipants = 20,
                currentActivity = "Review",
            ),
            StudyCircleDto(
                id = "circle-wahib-yasin",
                surahName = "Surah Yasin",
                hostId = "sheikh-wahib",
                hostName = "Sheikh Wahib",
                hostInitials = "وه",
                isLive = true,
                difficulty = "beginner",
                participantCount = 7,
                maxParticipants = 15,
                currentActivity = "Reading",
            ),
        )

        val SEED_DOCUMENTS = mapOf(
            "about" to LegalDocumentDto(
                type = "about",
                title = "عن الماهر",
                updatedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
                body = """
                    # الماهر

                    تطبيق الماهر رفيقك في تلاوة القرآن الكريم وحفظه.
                """.trimIndent(),
            ),
            "terms_of_service" to LegalDocumentDto(
                type = "terms_of_service",
                title = "شروط الخدمة",
                updatedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
                body = """
                    # شروط الخدمة

                    باستخدامك تطبيق الماهر فإنك توافق على الشروط الموضّحة أدناه.
                """.trimIndent(),
            ),
            "privacy_policy" to LegalDocumentDto(
                type = "privacy_policy",
                title = "سياسة الخصوصية",
                updatedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
                body = """
                    # سياسة الخصوصية

                    نحرص على حماية بياناتك ونجمع الحد الأدنى اللازم لتشغيل الخدمة.
                """.trimIndent(),
            ),
        )
    }
}
