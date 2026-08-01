package com.iti.presentation.profile

import com.iti.domain.model.LegalDocumentType
import com.iti.domain.repository.AlmahirRepository
import com.iti.domain.usecase.subscription.GetSubscriptionUseCase
import com.iti.domain.usecase.subscription.RestorePurchasesUseCase
import com.iti.domain.usecase.user.DeleteAccountUseCase
import com.iti.domain.usecase.user.GetCurrentUserUseCase
import com.iti.domain.auth.usecase.LogoutUseCase
import com.iti.presentation.R
import com.iti.presentation.profile.model.ProfileMenuType
import com.iti.presentation.profile.state.ProfileDialog
import com.iti.presentation.profile.state.ProfileEffect
import com.iti.presentation.profile.state.ProfileIntent
import com.iti.presentation.testing.FakeAlmahirRepository
import com.iti.presentation.testing.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `user and subscription streams are combined into one state`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAlmahirRepository())

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessageRes)
        assertEquals("jamal@example.com", state.user?.email)
        assertFalse(state.isPremium)
    }

    @Test
    fun `a premium entitlement is reflected in state`() = runTest(dispatcher) {
        val repository = FakeAlmahirRepository(
            subscription = FakeAlmahirRepository.PREMIUM_SUBSCRIPTION,
        )
        val viewModel = viewModel(repository)

        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isPremium)
    }

    @Test
    fun `a failing stream surfaces the error instead of hanging on loading`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAlmahirRepository(failSubscription = true))

            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoading)
            assertTrue(state.hasError)
        }

    @Test
    fun `logout only runs after the confirmation dialog is confirmed`() = runTest(dispatcher) {
        val authRepository = FakeAuthRepository()
        val viewModel = viewModel(FakeAlmahirRepository(), authRepository)
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(ProfileIntent.LogoutClicked)

        // Opening the dialog must not sign the user out on its own.
        assertEquals(ProfileDialog.LOGOUT, viewModel.state.value.dialog)
        assertFalse(authRepository.loggedOut)

        viewModel.onIntent(ProfileIntent.DialogConfirmed)
        testScheduler.advanceUntilIdle()

        assertTrue(authRepository.loggedOut)
        assertNull(viewModel.state.value.dialog)
        assertEquals(ProfileEffect.NavigateToAuth, viewModel.effect.first())
    }

    @Test
    fun `dismissing the logout dialog leaves the session intact`() = runTest(dispatcher) {
        val authRepository = FakeAuthRepository()
        val viewModel = viewModel(FakeAlmahirRepository(), authRepository)
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(ProfileIntent.LogoutClicked)
        viewModel.onIntent(ProfileIntent.DialogDismissed)
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.dialog)
        assertFalse(authRepository.loggedOut)
    }

    @Test
    fun `account deletion is confirmed separately from logout and also ends the session`() =
        runTest(dispatcher) {
            val repository = FakeAlmahirRepository()
            val authRepository = FakeAuthRepository()
            val viewModel = viewModel(repository, authRepository)
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.DeleteAccountClicked)
            assertEquals(ProfileDialog.DELETE_ACCOUNT, viewModel.state.value.dialog)

            viewModel.onIntent(ProfileIntent.DialogConfirmed)
            testScheduler.advanceUntilIdle()

            assertTrue(repository.deletedAccount)
            assertFalse(authRepository.loggedOut)
            assertEquals(ProfileEffect.NavigateToAuth, viewModel.effect.first())
        }

    @Test
    fun `a failed deletion keeps the user signed in and reports the failure`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAlmahirRepository(failDeleteAccount = true))
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.DeleteAccountClicked)
            viewModel.onIntent(ProfileIntent.DialogConfirmed)
            testScheduler.advanceUntilIdle()

            assertEquals(
                ProfileEffect.ShowMessage(R.string.profile_delete_account_failed),
                viewModel.effect.first(),
            )
            assertFalse(viewModel.state.value.isProcessingDialogAction)
        }

    @Test
    fun `the dialog cannot be dismissed while its action is in flight`() = runTest(dispatcher) {
        val repository = FakeAlmahirRepository()
        val viewModel = viewModel(repository)
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(ProfileIntent.DeleteAccountClicked)
        viewModel.onIntent(ProfileIntent.DialogConfirmed)

        // The deletion is running; a stray dismiss must not strand it or reopen the screen.
        viewModel.onIntent(ProfileIntent.DialogDismissed)
        assertEquals(ProfileDialog.DELETE_ACCOUNT, viewModel.state.value.dialog)

        testScheduler.advanceUntilIdle()
        assertTrue(repository.deletedAccount)
    }

    @Test
    fun `a repeated tap while restoring does not send a second request`() = runTest(dispatcher) {
        val repository = FakeAlmahirRepository()
        val viewModel = viewModel(repository)
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(ProfileIntent.RestorePurchasesClicked)
        assertTrue(viewModel.state.value.isRestoringPurchases)
        viewModel.onIntent(ProfileIntent.RestorePurchasesClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(1, repository.restoreCount)
        assertFalse(viewModel.state.value.isRestoringPurchases)
    }

    @Test
    fun `restoring nothing is reported distinctly from restoring something`() =
        runTest(dispatcher) {
            val viewModel = viewModel(FakeAlmahirRepository(restoreResult = true))
            testScheduler.advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.RestorePurchasesClicked)
            testScheduler.advanceUntilIdle()

            assertEquals(
                ProfileEffect.ShowMessage(R.string.profile_restore_succeeded),
                viewModel.effect.first(),
            )
        }

    @Test
    fun `menu options map to their destinations`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAlmahirRepository())
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(ProfileIntent.MenuOptionClicked(ProfileMenuType.PRIVACY_POLICY))

        assertEquals(
            ProfileEffect.OpenLegalDocument(LegalDocumentType.PRIVACY_POLICY),
            viewModel.effect.first(),
        )
    }

    @Test
    fun `share and rate stay in-app rather than opening a document`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAlmahirRepository())
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(ProfileIntent.MenuOptionClicked(ProfileMenuType.SHARE_APP))

        assertEquals(ProfileEffect.ShareApp, viewModel.effect.first())
    }

    private fun viewModel(
        repository: AlmahirRepository,
        authRepository: FakeAuthRepository = FakeAuthRepository(),
        appPreferencesRepository: com.iti.domain.settings.repository.AppPreferencesRepository = FakeAppPreferencesRepository(),
        connectivityObserver: com.iti.domain.connectivity.ConnectivityObserver = FakeConnectivityObserver(),
    ) = ProfileViewModel(
        getCurrentUser = GetCurrentUserUseCase(appPreferencesRepository),
        getSubscription = GetSubscriptionUseCase(repository),
        restorePurchases = RestorePurchasesUseCase(repository),
        logout = LogoutUseCase(authRepository),
        deleteAccount = DeleteAccountUseCase(repository),
        connectivityObserver = connectivityObserver,
    )

    private class FakeAppPreferencesRepository(
        user: com.iti.domain.model.User = FakeAlmahirRepository.USER
    ) : com.iti.domain.settings.repository.AppPreferencesRepository {
        val state = kotlinx.coroutines.flow.MutableStateFlow(com.iti.domain.settings.model.AppPreferences(user = user))
        override val preferences: kotlinx.coroutines.flow.Flow<com.iti.domain.settings.model.AppPreferences> = state
        override suspend fun setThemeMode(mode: com.iti.domain.settings.model.ThemeMode): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun setLanguage(language: com.iti.domain.settings.model.AppLanguage): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun setRemindersEnabled(enabled: Boolean): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun setErrorSoundsEnabled(enabled: Boolean): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun setDataSaverEnabled(enabled: Boolean): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun saveUser(user: com.iti.domain.model.User): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
        override suspend fun clearUser(): com.iti.domain.core.Result<Unit> = com.iti.domain.core.Result.Success(Unit)
    }

    private class FakeConnectivityObserver : com.iti.domain.connectivity.ConnectivityObserver {
        override val status: kotlinx.coroutines.flow.Flow<com.iti.domain.connectivity.ConnectivityStatus> = kotlinx.coroutines.flow.flowOf(com.iti.domain.connectivity.ConnectivityStatus.Available)
        override fun currentStatus(): com.iti.domain.connectivity.ConnectivityStatus = com.iti.domain.connectivity.ConnectivityStatus.Available
    }
}

