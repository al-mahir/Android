package com.iti.data.datasource

import com.iti.data.dto.LegalDocumentDto
import com.iti.data.dto.SubscriptionDto
import com.iti.data.dto.UserDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart


class AlmahirFakeDataSource : AlmahirDataSource {

    private val subscription = MutableStateFlow(SEED_SUBSCRIPTION)

    override fun observeCurrentUser(): Flow<UserDto> =
        flow { emit(SEED_USER) }.onStart { delay(USER_DELAY_MS) }

    override fun observeSubscription(): Flow<SubscriptionDto> =
        subscription.asStateFlow().onStart { delay(SUBSCRIPTION_DELAY_MS) }

    override fun observeLegalDocument(documentType: String): Flow<LegalDocumentDto> =
        flow {
            val document = SEED_DOCUMENTS[documentType]
                ?: error("Unknown legal document type: $documentType")
            emit(document)
        }.onStart { delay(DOCUMENT_DELAY_MS) }

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
        const val SUBSCRIPTION_DELAY_MS = 400L
        const val DOCUMENT_DELAY_MS = 600L
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
