package com.iti.domain.payment

import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.domain.model.Bookmark
import com.iti.domain.model.BookmarkType
import com.iti.domain.model.LegalDocument
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.model.Subscription
import com.iti.domain.model.SubscriptionPackage
import com.iti.domain.model.SubscriptionPlan
import com.iti.domain.model.User
import com.iti.domain.payment.usecase.ActivateSubscriptionAfterPaymentUseCase
import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.usecase.subscription.SelectSubscriptionPackageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivateSubscriptionAfterPaymentUseCaseTest {

    @Test
    fun `activates the subscription for the paid package`() = runTest {
        val repository = FakeSubscriptionRepository()
        val useCase = ActivateSubscriptionAfterPaymentUseCase(SelectSubscriptionPackageUseCase(repository))

        val result = useCase("pkg-intensive")

        assertEquals(listOf("pkg-intensive"), repository.selectedPackageIds)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `propagates a repository failure instead of hiding it`() = runTest {
        val repository = FakeSubscriptionRepository(shouldFail = true)
        val useCase = ActivateSubscriptionAfterPaymentUseCase(SelectSubscriptionPackageUseCase(repository))

        val result = useCase("pkg-intensive")

        assertTrue(result is Result.Error)
    }

    /** Only `selectSubscriptionPackage` is exercised by this use case — every other member is
     * unused by these tests and left unimplemented on purpose. */
    private class FakeSubscriptionRepository(
        private val shouldFail: Boolean = false,
    ) : AlmahirRepository {
        val selectedPackageIds = mutableListOf<String>()

        override suspend fun selectSubscriptionPackage(packageId: String): Result<Subscription> {
            selectedPackageIds += packageId
            return if (shouldFail) {
                Result.Error(DomainError.Unknown(IllegalStateException("boom")))
            } else {
                Result.Success(Subscription(plan = SubscriptionPlan.PREMIUM, renewsAtEpochMillis = null, activePackageId = packageId))
            }
        }

        override fun observeCurrentUser(): Flow<Result<User>> = notUsed()
        override fun observeSubscription(): Flow<Result<Subscription>> = notUsed()
        override fun observeSubscriptionPackages(): Flow<Result<List<SubscriptionPackage>>> = notUsed()
        override suspend fun startFreeTrial(): Result<Subscription> = notUsed()
        override fun observeLegalDocument(type: LegalDocumentType): Flow<Result<LegalDocument>> = notUsed()
        override suspend fun requestSubscriptionCancellation(message: String): Result<Unit> = notUsed()
        override suspend fun logout(): Result<Unit> = notUsed()
        override suspend fun deleteAccount(): Result<Unit> = notUsed()
        override fun observeBookmarks(type: BookmarkType): Flow<Result<List<Bookmark>>> = notUsed()
        override fun observeAllBookmarks(): Flow<Result<List<Bookmark>>> = notUsed()
        override suspend fun getBookmarks(type: BookmarkType): Result<List<Bookmark>> = notUsed()
        override suspend fun getBookmark(id: String): Result<Bookmark?> = notUsed()
        override suspend fun addBookmark(bookmark: Bookmark): Result<Unit> = notUsed()
        override suspend fun removeBookmark(id: String): Result<Unit> = notUsed()
        
        override fun observeMeetingStatuses(userId: String): Flow<Result<List<com.iti.domain.model.MeetingStatus>>> = notUsed()
        override suspend fun saveMeetingStatus(status: com.iti.domain.model.MeetingStatus): Result<Unit> = notUsed()

        private fun notUsed(): Nothing = error("not used by this test")
    }
}
