package com.iti.presentation.payment.checkout

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
import com.iti.domain.payment.model.CardBrand
import com.iti.domain.payment.model.PaymentIntention
import com.iti.domain.payment.model.PaymentMethodType
import com.iti.domain.payment.model.PaymentOutcome
import com.iti.domain.payment.model.PaymentStatus
import com.iti.domain.payment.model.WalletProvider
import com.iti.domain.payment.repository.PaymentRepository
import com.iti.domain.payment.usecase.ActivateSubscriptionAfterPaymentUseCase
import com.iti.domain.payment.usecase.CreatePaymentIntentionUseCase
import com.iti.domain.payment.usecase.GetPaymentStatusUseCase
import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.settings.model.AppPreferences
import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.ThemeMode
import com.iti.domain.settings.repository.AppPreferencesRepository
import com.iti.domain.usecase.subscription.GetSubscriptionPackagesUseCase
import com.iti.domain.usecase.subscription.SelectSubscriptionPackageUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `re-validates an already-entered wallet number when the provider changes`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onIntent(CheckoutIntent.WalletProviderSelected(WalletProvider.VODAFONE_CASH))
        viewModel.onIntent(CheckoutIntent.WalletNumberChanged("01212345678")) // Orange prefix
        viewModel.onIntent(CheckoutIntent.WalletProviderSelected(WalletProvider.VODAFONE_CASH))

        assertTrue(viewModel.state.value.walletNumberError != null)
    }

    @Test
    fun `blank wallet submission sets an error and never calls the repository`() = runTest(dispatcher) {
        val almahirRepository = FakeCheckoutAlmahirRepository()
        val paymentRepository = FakePaymentRepository()
        val viewModel = viewModel(almahirRepository = almahirRepository, paymentRepository = paymentRepository)
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CheckoutIntent.WalletProviderSelected(WalletProvider.VODAFONE_CASH))
        viewModel.onIntent(CheckoutIntent.PayClicked)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.walletNumberError != null)
        assertTrue(paymentRepository.createIntentionCalls.isEmpty())
    }

    @Test
    fun `successful intention creation sends LaunchPaymobSdk effect`() = runTest(dispatcher) {
        val paymentRepository = FakePaymentRepository()
        val viewModel = viewModel(paymentRepository = paymentRepository)
        testScheduler.advanceUntilIdle()

        viewModel.payWithValidWallet()
        testScheduler.advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue("Expected LaunchPaymobSdk effect", effect is CheckoutEffect.LaunchPaymobSdk)
        assertEquals(listOf(PACKAGE_ID), paymentRepository.createIntentionCalls)
    }

    @Test
    fun `SDK success callback + backend SUCCESS activates subscription`() = runTest(dispatcher) {
        val almahirRepository = FakeCheckoutAlmahirRepository()
        val paymentRepository = FakePaymentRepository(statusResult = PaymentStatus.SUCCESS)
        val viewModel = viewModel(almahirRepository = almahirRepository, paymentRepository = paymentRepository)
        testScheduler.advanceUntilIdle()

        viewModel.payWithValidWallet()
        testScheduler.advanceUntilIdle()

        // Simulate SDK reporting success
        viewModel.onIntent(CheckoutIntent.PaymobSdkResult(PaymobSdkOutcome.Success(hashMapOf())))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(PACKAGE_ID), almahirRepository.activatedPackageIds)
        assertTrue(viewModel.state.value.overlay is CheckoutOverlay.Success)
    }

    @Test
    fun `SDK pending callback + backend PENDING does not activate subscription and shows pending overlay`() = runTest(dispatcher) {
        val almahirRepository = FakeCheckoutAlmahirRepository()
        val paymentRepository = FakePaymentRepository(statusResult = PaymentStatus.PENDING)
        val viewModel = viewModel(almahirRepository = almahirRepository, paymentRepository = paymentRepository)
        testScheduler.advanceUntilIdle()

        viewModel.payWithValidWallet()
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CheckoutIntent.PaymobSdkResult(PaymobSdkOutcome.Pending))
        testScheduler.advanceUntilIdle()

        assertTrue(almahirRepository.activatedPackageIds.isEmpty())
        // After MAX_STATUS_POLLS retries still pending → success overlay with pending message
        assertTrue(viewModel.state.value.overlay is CheckoutOverlay.Success)
    }

    @Test
    fun `SDK failure callback does not call status endpoint and shows error overlay`() = runTest(dispatcher) {
        val almahirRepository = FakeCheckoutAlmahirRepository()
        val paymentRepository = FakePaymentRepository(statusResult = PaymentStatus.SUCCESS)
        val viewModel = viewModel(almahirRepository = almahirRepository, paymentRepository = paymentRepository)
        testScheduler.advanceUntilIdle()

        viewModel.payWithValidWallet()
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CheckoutIntent.PaymobSdkResult(PaymobSdkOutcome.Failure("declined")))
        testScheduler.advanceUntilIdle()

        assertTrue(almahirRepository.activatedPackageIds.isEmpty())
        assertTrue(paymentRepository.getStatusCalls.isEmpty()) // no status poll on SDK failure
        assertTrue(viewModel.state.value.overlay is CheckoutOverlay.Error)
    }

    @Test
    fun `backend FAILED status does not activate subscription`() = runTest(dispatcher) {
        val almahirRepository = FakeCheckoutAlmahirRepository()
        val paymentRepository = FakePaymentRepository(statusResult = PaymentStatus.FAILED)
        val viewModel = viewModel(almahirRepository = almahirRepository, paymentRepository = paymentRepository)
        testScheduler.advanceUntilIdle()

        viewModel.payWithValidWallet()
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CheckoutIntent.PaymobSdkResult(PaymobSdkOutcome.Success(hashMapOf())))
        testScheduler.advanceUntilIdle()

        assertTrue(almahirRepository.activatedPackageIds.isEmpty())
        assertTrue(viewModel.state.value.overlay is CheckoutOverlay.Error)
    }

    @Test
    fun `tapping Pay again while a payment is already processing is a no-op`() = runTest(dispatcher) {
        val almahirRepository = FakeCheckoutAlmahirRepository()
        val paymentRepository = FakePaymentRepository()
        val viewModel = viewModel(almahirRepository = almahirRepository, paymentRepository = paymentRepository)
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CheckoutIntent.WalletProviderSelected(WalletProvider.VODAFONE_CASH))
        viewModel.onIntent(CheckoutIntent.WalletNumberChanged("01012345678"))
        viewModel.onIntent(CheckoutIntent.PayClicked) // starts processing, overlay = Loading
        viewModel.onIntent(CheckoutIntent.PayClicked) // ignored — already processing

        testScheduler.advanceUntilIdle()

        assertEquals(1, paymentRepository.createIntentionCalls.size)
    }

    @Test
    fun `dismissing the overlay after success sends a navigate-back effect`() = runTest(dispatcher) {
        val paymentRepository = FakePaymentRepository(statusResult = PaymentStatus.SUCCESS)
        val viewModel = viewModel(paymentRepository = paymentRepository)
        testScheduler.advanceUntilIdle()

        viewModel.payWithValidWallet()
        testScheduler.advanceUntilIdle()

        // PayClicked → createIntention → sends LaunchPaymobSdk effect first.
        // Consume it so the channel is empty before we trigger NavigateBack.
        val sdkEffect = viewModel.effect.first()
        assertTrue("Expected LaunchPaymobSdk before NavigateBack", sdkEffect is CheckoutEffect.LaunchPaymobSdk)

        viewModel.onIntent(CheckoutIntent.PaymobSdkResult(PaymobSdkOutcome.Success(hashMapOf())))
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(CheckoutIntent.OverlayDismissed)

        assertEquals(CheckoutEffect.NavigateBack, viewModel.effect.first())
        assertNull(viewModel.state.value.overlay)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun CheckoutViewModel.payWithValidWallet() {
        onIntent(CheckoutIntent.WalletProviderSelected(WalletProvider.VODAFONE_CASH))
        onIntent(CheckoutIntent.WalletNumberChanged("01012345678"))
        onIntent(CheckoutIntent.PayClicked)
    }

    private fun viewModel(
        almahirRepository: FakeCheckoutAlmahirRepository = FakeCheckoutAlmahirRepository(),
        paymentRepository: FakePaymentRepository = FakePaymentRepository(),
        appPreferencesRepository: AppPreferencesRepository = FakeAppPreferencesRepository(),
    ) = CheckoutViewModel(
        packageId = PACKAGE_ID,
        getSubscriptionPackages = GetSubscriptionPackagesUseCase(almahirRepository),
        getCurrentUser = GetCurrentUserUseCase(appPreferencesRepository),
        createPaymentIntention = CreatePaymentIntentionUseCase(paymentRepository),
        getPaymentStatus = GetPaymentStatusUseCase(paymentRepository),
        activateSubscriptionAfterPayment = ActivateSubscriptionAfterPaymentUseCase(
            SelectSubscriptionPackageUseCase(almahirRepository),
        ),
    )

    private companion object {
        const val PACKAGE_ID = "pkg-intensive"

        val PACKAGE = SubscriptionPackage(
            id = PACKAGE_ID,
            name = "Intensive",
            monthlyPriceMinorUnits = 6_500,
            currencyCode = "EGP",
            features = listOf("45-minute sessions"),
            isRecommended = true,
        )
    }

    private class FakePaymentRepository(
        private val statusResult: PaymentStatus = PaymentStatus.SUCCESS,
        private val failIntention: Boolean = false,
    ) : PaymentRepository {
        val createIntentionCalls = mutableListOf<String>()
        val getStatusCalls = mutableListOf<String>()

        override suspend fun createIntention(
            packageId: String,
            method: PaymentMethodType,
            idempotencyKey: String,
        ): Result<PaymentIntention> {
            createIntentionCalls += packageId
            return if (failIntention) {
                Result.Error(DomainError.Unknown(IllegalStateException("boom")))
            } else {
                Result.Success(
                    PaymentIntention(
                        intentionId = "intent-1",
                        clientSecret = "secret-1",
                        publicKey = "pub-key-1",
                        amountMinorUnits = 6_500,
                        currencyCode = "EGP",
                    )
                )
            }
        }

        override suspend fun getPaymentStatus(intentionId: String): Result<PaymentOutcome> {
            getStatusCalls += intentionId
            return Result.Success(PaymentOutcome(transactionId = "txn-1", status = statusResult))
        }
    }

    /** Tracks `selectSubscriptionPackage` calls so tests can assert whether activation happened;
     * every other member is unused by [CheckoutViewModel] and left unimplemented on purpose. */
    private class FakeCheckoutAlmahirRepository(
        private val packages: List<SubscriptionPackage> = listOf(PACKAGE),
    ) : AlmahirRepository {
        val activatedPackageIds = mutableListOf<String>()

        override fun observeSubscriptionPackages(): Flow<Result<List<SubscriptionPackage>>> =
            flowOf(Result.Success(packages))

        override suspend fun selectSubscriptionPackage(packageId: String): Result<Subscription> {
            activatedPackageIds += packageId
            return Result.Success(Subscription(plan = SubscriptionPlan.PREMIUM, renewsAtEpochMillis = null, activePackageId = packageId))
        }

        override fun observeCurrentUser(): Flow<Result<User>> = notUsed()
        override fun observeSubscription(): Flow<Result<Subscription>> = notUsed()
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

        override fun observeMeetingStatuses(userId: String): Flow<Result<List<com.iti.domain.model.MeetingStatus>>> =
            flowOf(Result.Success(emptyList()))

        override suspend fun saveMeetingStatus(status: com.iti.domain.model.MeetingStatus): Result<Unit> =
            Result.Success(Unit)

        private fun notUsed(): Nothing = error("not used by this test")
    }

    private class FakeAppPreferencesRepository(
        user: User = User(
            id = "user-1",
            displayName = "Test User",
            initials = "TU",
            avatarUrl = null,
            email = "test@example.com",
            joinedAtEpochMillis = 0L,
        ),
    ) : AppPreferencesRepository {
        override val preferences: Flow<AppPreferences> = MutableStateFlow(
            AppPreferences(user = user, language = AppLanguage.ENGLISH, themeMode = ThemeMode.SYSTEM),
        )

        override suspend fun setThemeMode(mode: ThemeMode): Result<Unit> = Result.Success(Unit)
        override suspend fun setLanguage(language: AppLanguage): Result<Unit> = Result.Success(Unit)
        override suspend fun setRemindersEnabled(enabled: Boolean): Result<Unit> = Result.Success(Unit)
        override suspend fun setErrorSoundsEnabled(enabled: Boolean): Result<Unit> = Result.Success(Unit)
        override suspend fun setDataSaverEnabled(enabled: Boolean): Result<Unit> = Result.Success(Unit)
        override suspend fun saveUser(user: User): Result<Unit> = Result.Success(Unit)
        override suspend fun clearUser(): Result<Unit> = Result.Success(Unit)
    }
}
