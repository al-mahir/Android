package com.iti.presentation.auth

import com.iti.domain.auth.usecase.LoginUseCase
import com.iti.domain.auth.usecase.LoginWithGoogleUseCase
import com.iti.domain.auth.usecase.RegisterUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.R
import com.iti.presentation.auth.register.RegisterEffect
import com.iti.presentation.auth.register.RegisterIntent
import com.iti.presentation.auth.register.RegisterViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a new account is signed in straight away and lands on home`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.submitValidForm()
        testScheduler.advanceUntilIdle()

        // Registration returns no tokens, so the session comes from the follow-up sign-in.
        assertEquals(listOf(EMAIL), repository.registrations)
        assertEquals(listOf(EMAIL), repository.logins)
        assertEquals(RegisterEffect.NavigateToHome, viewModel.effect.first())
    }

    @Test
    fun `an account that cannot be signed in sends the user to login`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(loginResult = FakeAuthRepository.failure())
        val viewModel = viewModel(repository)

        viewModel.submitValidForm()
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(EMAIL), repository.registrations)
        assertEquals(
            RegisterEffect.NavigateToLogin(
                UiText.Resource(R.string.auth_error_registered_sign_in_failed)
            ),
            viewModel.effect.first(),
        )
    }

    @Test
    fun `field problems are pinned to their fields and stop the request`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.submitValidForm(email = "not-an-email", password = "weak")
        testScheduler.advanceUntilIdle()

        assertTrue(repository.registrations.isEmpty())
        val state = viewModel.state.value
        assertEquals(UiText.Resource(R.string.auth_error_invalid_email), state.emailError)
        assertEquals(UiText.Resource(R.string.auth_error_weak_password), state.passwordError)
    }

    @Test
    fun `a taken email reported by the server is shown as a message`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(
            registerResult = Result.Error(DomainError.ConflictError("Email already in use")),
        )
        val viewModel = viewModel(repository)

        viewModel.submitValidForm()
        testScheduler.advanceUntilIdle()

        assertTrue(repository.logins.isEmpty())
        assertEquals(
            RegisterEffect.ShowError(UiText.Resource(R.string.auth_error_conflict)),
            viewModel.effect.first(),
        )
    }

    private fun RegisterViewModel.submitValidForm(
        email: String = EMAIL,
        password: String = PASSWORD,
    ) {
        onIntent(RegisterIntent.UsernameChanged("mahir"))
        onIntent(RegisterIntent.FirstNameChanged("Yasser"))
        onIntent(RegisterIntent.LastNameChanged("Ali"))
        onIntent(RegisterIntent.PhoneNumberChanged("1247170592"))
        onIntent(RegisterIntent.GenderChanged("MALE"))
        onIntent(RegisterIntent.EmailChanged(email))
        onIntent(RegisterIntent.PasswordChanged(password))
        onIntent(RegisterIntent.SubmitRegistration)
    }

    private fun viewModel(repository: FakeAuthRepository) = RegisterViewModel(
        registerUseCase = RegisterUseCase(repository),
        loginUseCase = LoginUseCase(repository),
        loginWithGoogleUseCase = LoginWithGoogleUseCase(repository),
    )

    private companion object {
        const val EMAIL = "yasser@example.com"
        const val PASSWORD = "Passw0rd!"
    }
}
