package com.iti.presentation.auth

import com.iti.domain.auth.usecase.LoginUseCase
import com.iti.domain.auth.usecase.LoginWithGoogleUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.R
import com.iti.presentation.auth.login.LoginEffect
import com.iti.presentation.auth.login.LoginIntent
import com.iti.presentation.auth.login.LoginViewModel
import com.example.designsystem.text.UiText
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `valid credentials sign the user in and open home`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.onIntent(LoginIntent.EmailChanged(EMAIL))
        viewModel.onIntent(LoginIntent.PasswordChanged(PASSWORD))
        viewModel.onIntent(LoginIntent.SubmitLogin)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(EMAIL), repository.logins)
        assertEquals(LoginEffect.NavigateToHome, viewModel.effect.first())
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `a malformed email is reported on the field and never reaches the network`() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = viewModel(repository)

            viewModel.onIntent(LoginIntent.EmailChanged("not-an-email"))
            viewModel.onIntent(LoginIntent.PasswordChanged(PASSWORD))
            viewModel.onIntent(LoginIntent.SubmitLogin)
            testScheduler.advanceUntilIdle()

            assertTrue(repository.logins.isEmpty())
            assertEquals(
                UiText.Resource(R.string.auth_error_invalid_email),
                viewModel.state.value.emailError,
            )
        }

    @Test
    fun `rejected credentials surface as a message rather than a field error`() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository(
                loginResult = Result.Error(DomainError.Unauthorized("Invalid credentials")),
            )
            val viewModel = viewModel(repository)

            viewModel.onIntent(LoginIntent.EmailChanged(EMAIL))
            viewModel.onIntent(LoginIntent.PasswordChanged(PASSWORD))
            viewModel.onIntent(LoginIntent.SubmitLogin)
            testScheduler.advanceUntilIdle()

            assertNull(viewModel.state.value.emailError)
            assertEquals(
                LoginEffect.ShowError(UiText.Dynamic("Invalid credentials")),
                viewModel.effect.first(),
            )
        }

    @Test
    fun `a dropped connection is reported in the app's own words`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(
            loginResult = Result.Error(DomainError.NetworkError(IllegalStateException("offline"))),
        )
        val viewModel = viewModel(repository)

        viewModel.onIntent(LoginIntent.EmailChanged(EMAIL))
        viewModel.onIntent(LoginIntent.PasswordChanged(PASSWORD))
        viewModel.onIntent(LoginIntent.SubmitLogin)
        testScheduler.advanceUntilIdle()

        assertEquals(
            LoginEffect.ShowError(UiText.Resource(R.string.error_network)),
            viewModel.effect.first(),
        )
    }

    @Test
    fun `the google button asks the screen for a token instead of inventing one`() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = viewModel(repository)

            viewModel.onIntent(LoginIntent.GoogleSignInClicked)
            testScheduler.advanceUntilIdle()

            // Nothing is sent until a real token comes back from Credential Manager.
            assertTrue(repository.googleTokens.isEmpty())
            assertTrue(viewModel.state.value.isLoading)
            assertEquals(LoginEffect.LaunchGoogleSignIn, viewModel.effect.first())
        }

    @Test
    fun `a google token is exchanged for a session`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.onIntent(LoginIntent.GoogleTokenReceived("google-id-token"))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("google-id-token"), repository.googleTokens)
        assertEquals(LoginEffect.NavigateToHome, viewModel.effect.first())
    }

    @Test
    fun `dismissing the google sheet just stops the spinner`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAuthRepository())

        viewModel.onIntent(LoginIntent.GoogleSignInClicked)
        viewModel.onIntent(LoginIntent.GoogleSignInDismissed)
        testScheduler.advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `an unconfigured google sign-in says so instead of failing validation`() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = viewModel(repository)

            viewModel.onIntent(LoginIntent.GoogleSignInUnavailable)
            testScheduler.advanceUntilIdle()

            assertTrue(repository.googleTokens.isEmpty())
            assertEquals(
                LoginEffect.ShowError(UiText.Resource(R.string.auth_error_google_unavailable)),
                viewModel.effect.first(),
            )
        }

    private fun viewModel(repository: FakeAuthRepository) = LoginViewModel(
        loginUseCase = LoginUseCase(repository),
        loginWithGoogleUseCase = LoginWithGoogleUseCase(repository),
    )

    private companion object {
        const val EMAIL = "yasser@example.com"
        const val PASSWORD = "Passw0rd!"
    }
}
