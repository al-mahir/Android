package com.iti.data.datasource

import com.iti.data.dto.LegalDocumentDto
import com.iti.data.dto.SubscriptionDto
import com.iti.data.dto.SubscriptionPackageDto
import com.iti.data.dto.UserDto
import com.iti.data.settings.local.AppPreferencesDataStore
import com.iti.domain.settings.model.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update


class AlmahirFakeDataSource(
    private val appPreferencesDataStore: AppPreferencesDataStore,
) : AlmahirDataSource {

    private val subscription = MutableStateFlow(SEED_SUBSCRIPTION)

    override fun observeCurrentUser(): Flow<UserDto> =
        flow { emit(SEED_USER) }.onStart { delay(USER_DELAY_MS) }

    override fun observeSubscription(): Flow<SubscriptionDto> =
        subscription.asStateFlow().onStart { delay(SUBSCRIPTION_DELAY_MS) }

    override fun observeSubscriptionPackages(): Flow<List<SubscriptionPackageDto>> =
        currentLanguage()
            .map { language -> packagesFor(language) }
            .onStart { delay(PACKAGES_DELAY_MS) }

    override suspend fun startFreeTrial(): SubscriptionDto {
        delay(TRIAL_DELAY_MS)
        subscription.update {
            it.copy(
                plan = "premium",
                renewsAtEpochMillis = System.currentTimeMillis() + TRIAL_DURATION_MS,
                activePackageId = TRIAL_PACKAGE_ID,
            )
        }
        return subscription.value
    }

    override suspend fun selectSubscriptionPackage(packageId: String): SubscriptionDto {
        require(PACKAGE_IDS.contains(packageId)) { "Unknown package id: $packageId" }
        delay(SELECT_PACKAGE_DELAY_MS)
        subscription.update {
            it.copy(
                plan = "premium",
                renewsAtEpochMillis = System.currentTimeMillis() + RENEWAL_PERIOD_MS,
                activePackageId = packageId,
            )
        }
        return subscription.value
    }

    override fun observeLegalDocument(documentType: String): Flow<LegalDocumentDto> =
        currentLanguage()
            .map { language -> documentFor(documentType, language) }
            .onStart { delay(DOCUMENT_DELAY_MS) }

    override suspend fun requestSubscriptionCancellation(message: String) {
        delay(CANCELLATION_REQUEST_DELAY_MS)
    }

    override suspend fun logout() {
        delay(LOGOUT_DELAY_MS)
    }

    override suspend fun deleteAccount() {
        delay(DELETE_ACCOUNT_DELAY_MS)
    }

    private fun currentLanguage(): Flow<AppLanguage> =
        appPreferencesDataStore.preferences.map { it.language }.distinctUntilChanged()

    private fun packagesFor(language: AppLanguage): List<SubscriptionPackageDto> = when (language) {
        AppLanguage.ARABIC -> SEED_PACKAGES_AR
        AppLanguage.ENGLISH -> SEED_PACKAGES_EN
    }

    private fun documentFor(documentType: String, language: AppLanguage): LegalDocumentDto {
        val documents = when (language) {
            AppLanguage.ARABIC -> SEED_DOCUMENTS_AR
            AppLanguage.ENGLISH -> SEED_DOCUMENTS_EN
        }
        return documents[documentType] ?: error("Unknown legal document type: $documentType")
    }

    private companion object {
        const val USER_DELAY_MS = 300L
        const val SUBSCRIPTION_DELAY_MS = 400L
        const val PACKAGES_DELAY_MS = 500L
        const val TRIAL_DELAY_MS = 700L
        const val SELECT_PACKAGE_DELAY_MS = 900L
        const val DOCUMENT_DELAY_MS = 600L
        const val CANCELLATION_REQUEST_DELAY_MS = 900L
        const val LOGOUT_DELAY_MS = 500L
        const val DELETE_ACCOUNT_DELAY_MS = 1_200L

        const val JOINED_AT_EPOCH_MILLIS = 1_783_987_200_000L

        const val DAY_MS = 24 * 60 * 60 * 1_000L
        const val TRIAL_DURATION_MS = 7 * DAY_MS
        const val RENEWAL_PERIOD_MS = 30 * DAY_MS

        const val PACKAGE_ID_LIGHT = "pkg-light"
        const val PACKAGE_ID_INTENSIVE = "pkg-intensive"
        const val PACKAGE_ID_ELITE = "pkg-elite"

        val PACKAGE_IDS = setOf(PACKAGE_ID_LIGHT, PACKAGE_ID_INTENSIVE, PACKAGE_ID_ELITE)
        const val TRIAL_PACKAGE_ID = PACKAGE_ID_INTENSIVE

        val SEED_USER = UserDto(
            id = "user-1",
            displayName = "yassenRamadan1",
            email = "yassenr.hassan@gmail.com",
            joinedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
        )

        val SEED_SUBSCRIPTION = SubscriptionDto(plan = "none")

        val SEED_PACKAGES_EN = listOf(
            SubscriptionPackageDto(
                id = PACKAGE_ID_LIGHT,
                name = "Light",
                monthlyPriceMinorUnits = 4_000,
                currencyCode = "EGP",
                features = listOf(
                    "30-minute sessions",
                    "Weekly progress report",
                    "Group correction sessions",
                ),
                isRecommended = false,
            ),
            SubscriptionPackageDto(
                id = PACKAGE_ID_INTENSIVE,
                name = "Intensive",
                monthlyPriceMinorUnits = 6_500,
                currencyCode = "EGP",
                features = listOf(
                    "45-minute sessions",
                    "Personalized revision plan",
                    "Direct feedback + recordings",
                    "Priority scheduling",
                ),
                isRecommended = true,
            ),
            SubscriptionPackageDto(
                id = PACKAGE_ID_ELITE,
                name = "Elite",
                monthlyPriceMinorUnits = 12_000,
                currencyCode = "EGP",
                features = listOf(
                    "60-minute sessions",
                    "1-on-1 dedicated sheikh",
                    "Unlimited feedback + recordings",
                    "Priority scheduling",
                ),
                isRecommended = false,
            ),
        )

        val SEED_PACKAGES_AR = listOf(
            SubscriptionPackageDto(
                id = PACKAGE_ID_LIGHT,
                name = "الأساسية",
                monthlyPriceMinorUnits = 4_000,
                currencyCode = "EGP",
                features = listOf(
                    "حصص مدتها 30 دقيقة",
                    "تقرير تقدم أسبوعي",
                    "جلسات تصحيح جماعية",
                ),
                isRecommended = false,
            ),
            SubscriptionPackageDto(
                id = PACKAGE_ID_INTENSIVE,
                name = "مكثفة",
                monthlyPriceMinorUnits = 6_500,
                currencyCode = "EGP",
                features = listOf(
                    "حصص مدتها 45 دقيقة",
                    "خطة مراجعة شخصية",
                    "تغذية راجعة مباشرة + تسجيلات",
                    "جدولة ذات أولوية",
                ),
                isRecommended = true,
            ),
            SubscriptionPackageDto(
                id = PACKAGE_ID_ELITE,
                name = "النخبة",
                monthlyPriceMinorUnits = 12_000,
                currencyCode = "EGP",
                features = listOf(
                    "حصص مدتها 60 دقيقة",
                    "شيخ مخصص فردي",
                    "تغذية راجعة وتسجيلات غير محدودة",
                    "جدولة ذات أولوية",
                ),
                isRecommended = false,
            ),
        )

        val SEED_DOCUMENTS_AR = mapOf(
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

        val SEED_DOCUMENTS_EN = mapOf(
            "about" to LegalDocumentDto(
                type = "about",
                title = "About Al-Mahir",
                updatedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
                body = """
                    # Al-Mahir

                    Al-Mahir is your companion for reciting and memorizing the Holy Quran.
                """.trimIndent(),
            ),
            "terms_of_service" to LegalDocumentDto(
                type = "terms_of_service",
                title = "Terms of Service",
                updatedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
                body = """
                    # Terms of Service

                    By using the Al-Mahir app, you agree to the terms outlined below.
                """.trimIndent(),
            ),
            "privacy_policy" to LegalDocumentDto(
                type = "privacy_policy",
                title = "Privacy Policy",
                updatedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
                body = """
                    # Privacy Policy

                    We are committed to protecting your data and collect only the minimum
                    necessary to operate the service.
                """.trimIndent(),
            ),
        )
    }
}
