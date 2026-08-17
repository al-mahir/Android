package com.iti.data.datasource

import com.iti.data.dto.LegalDocumentDto
import com.iti.data.dto.SubscriptionDto
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

    /**
     * Accepts any [packageId]: package codes now come from `GET /api/payment/packages`, so this
     * fake cannot know the valid set and must not reject a code the backend just sold.
     */
    override suspend fun selectSubscriptionPackage(packageId: String): SubscriptionDto {
        require(packageId.isNotBlank()) { "Package id must not be blank" }
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
        const val SELECT_PACKAGE_DELAY_MS = 900L
        const val DOCUMENT_DELAY_MS = 600L
        const val CANCELLATION_REQUEST_DELAY_MS = 900L
        const val LOGOUT_DELAY_MS = 500L
        const val DELETE_ACCOUNT_DELAY_MS = 1_200L

        const val JOINED_AT_EPOCH_MILLIS = 1_783_987_200_000L

        const val DAY_MS = 24 * 60 * 60 * 1_000L
        const val RENEWAL_PERIOD_MS = 30 * DAY_MS

        val SEED_USER = UserDto(
            id = "user-1",
            displayName = "yassenRamadan1",
            email = "yassenr.hassan@gmail.com",
            joinedAtEpochMillis = JOINED_AT_EPOCH_MILLIS,
        )

        val SEED_SUBSCRIPTION = SubscriptionDto(plan = "none")

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
