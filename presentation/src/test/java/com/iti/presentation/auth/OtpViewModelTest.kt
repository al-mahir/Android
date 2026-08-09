package com.iti.presentation.auth

import com.example.designsystem.text.UiText
import com.iti.domain.auth.usecase.ForgotPasswordUseCase
import com.iti.domain.auth.usecase.LoginUseCase
import com.iti.domain.auth.usecase.VerifyOtpUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.R
import com.iti.presentation.auth.otp.OtpEffect
import com.iti.presentation.auth.otp.OtpFlow
import com.iti.presentation.auth.otp.OtpIntent
import com.iti.presentation.auth.otp.OtpViewModel
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
class OtpViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `valid 6-digit otp navigates to reset password`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.onIntent(OtpIntent.InitEmail("user@example.com", OtpFlow.FORGOT_PASSWORD))
        viewModel.onIntent(OtpIntent.OtpChanged("123456"))
        viewModel.onIntent(OtpIntent.Submit)
        testScheduler.advanceUntilIdle()

        assertEquals(
            OtpEffect.NavigateToResetPassword("user@example.com"),
            viewModel.effect.first()
        )
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `valid otp on email verification flow navigates to login`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.onIntent(OtpIntent.InitEmail("user@example.com", OtpFlow.EMAIL_VERIFICATION))
        viewModel.setCredentials("Passw0rd!")
        viewModel.onIntent(OtpIntent.OtpChanged("123456"))
        viewModel.onIntent(OtpIntent.Submit)
        testScheduler.advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is OtpEffect.NavigateToLogin)
    }

    @Test
    fun `invalid length otp shows error without network call`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.onIntent(OtpIntent.InitEmail("user@example.com"))
        viewModel.onIntent(OtpIntent.OtpChanged("123"))
        viewModel.onIntent(OtpIntent.Submit)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isError)
        assertEquals(
            UiText.Resource(R.string.auth_error_invalid_otp),
            viewModel.state.value.errorMessage
        )
    }

    @Test
    fun `resend otp resets timer and invokes forgot password`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = viewModel(repository)

        viewModel.onIntent(OtpIntent.InitEmail("user@example.com"))
        viewModel.onIntent(OtpIntent.ResendOtp)
        testScheduler.runCurrent()

        assertEquals(60, viewModel.state.value.timerSeconds)
        assertEquals(false, viewModel.state.value.canResend)
    }

    private fun viewModel(repository: FakeAuthRepository) = OtpViewModel(
        verifyOtpUseCase = VerifyOtpUseCase(repository),
        forgotPasswordUseCase = ForgotPasswordUseCase(repository),
        loginUseCase = LoginUseCase(repository),
    )
}
