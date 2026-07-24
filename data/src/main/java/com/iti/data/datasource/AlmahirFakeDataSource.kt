package com.iti.data.datasource

import com.iti.data.dto.LegalDocumentDto
import com.iti.data.dto.ReadingProgressDto
import com.iti.data.dto.SheikhDto
import com.iti.data.dto.StudyCircleDto
import com.iti.data.dto.SubscriptionDto
import com.iti.data.dto.UserDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
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
                if (circle.id == circleId) circle.copy(isJoined = true) else circle
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

        /** 2026-07-14T00:00:00Z. */
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

        val SEED_DOCUMENTS = mapOf(
            "about" to LegalDocumentDto(
                type = "about",
                title = "عن الماهر",
                updatedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
                body = """
                    # الماهر

                    تطبيق الماهر رفيقك في تلاوة القرآن الكريم وحفظه، يجمع بين المصحف
                    الرقمي وتصحيح التلاوة بالذكاء الاصطناعي وحلقات التعلّم مع الشيوخ.

                    ## ما الذي يميّزنا

                    - مصحف بخطوط المدينة الرسمية مع التجويد الملوّن.
                    - تصحيح فوري للتلاوة أثناء القراءة.
                    - حلقات مباشرة مع شيوخ معتمدين.
                    - متابعة لتقدّمك في الحفظ والمراجعة.

                    ## تواصل معنا

                    يسعدنا سماع رأيك واقتراحاتك عبر مركز المساعدة داخل التطبيق.
                """.trimIndent(),
            ),
            "terms_of_service" to LegalDocumentDto(
                type = "terms_of_service",
                title = "شروط الخدمة",
                updatedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
                body = """
                    # شروط الخدمة

                    باستخدامك تطبيق الماهر فإنك توافق على الشروط الموضّحة أدناه.

                    ## استخدام الحساب

                    - أنت مسؤول عن الحفاظ على سرية بيانات دخولك.
                    - يُمنع استخدام التطبيق لأي غرض مخالف للأنظمة المعمول بها.

                    ## الاشتراكات

                    - تُجدَّد الاشتراكات تلقائياً ما لم يتم إلغاؤها قبل موعد التجديد.
                    - تتم إدارة المدفوعات عبر متجر التطبيقات، وتخضع لسياساته.

                    ## إنهاء الخدمة

                    يحق لنا تعليق الحساب عند مخالفة هذه الشروط، مع إشعارك بذلك.
                """.trimIndent(),
            ),
            "privacy_policy" to LegalDocumentDto(
                type = "privacy_policy",
                title = "سياسة الخصوصية",
                updatedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
                body = """
                    # سياسة الخصوصية

                    نحرص على حماية بياناتك ونجمع الحد الأدنى اللازم لتشغيل الخدمة.

                    ## البيانات التي نجمعها

                    - بيانات الحساب: الاسم والبريد الإلكتروني.
                    - بيانات التلاوة: التسجيلات الصوتية المستخدمة للتصحيح.
                    - بيانات التقدّم: الصفحات المقروءة ونتائج المراجعة.

                    ## حقوقك

                    - يمكنك حذف تسجيلاتك الصوتية مع الاحتفاظ بإحصاءات تقدّمك.
                    - يمكنك طلب نسخة من بياناتك الشخصية أو حذف حسابك نهائياً.

                    ## الأمان

                    تُشفَّر البيانات أثناء النقل وعند التخزين، ولا نشاركها مع أطراف
                    ثالثة لأغراض إعلانية.
                """.trimIndent(),
            ),
        )
    }
}
