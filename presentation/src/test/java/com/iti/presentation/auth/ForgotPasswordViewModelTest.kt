package com.iti.presentation.auth

import com.example.designsystem.text.UiText
import com.iti.domain.auth.usecase.ForgotPasswordUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.R
import com.iti.presentation.auth.forgotpassword.ForgotPasswordEffect
import com.iti.presentation.auth.forgotpassword.ForgotPasswordIntent
import com.iti.presentation.auth.forgotpassword.ForgotPasswordViewModel
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `submitting valid email navigates to otp verify`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = ForgotPasswordViewModel(ForgotPasswordUseCase(repository))

        viewModel.onIntent(ForgotPasswordIntent.EmailChanged("user@example.com"))
        viewModel.onIntent(ForgotPasswordIntent.Submit)
        testScheduler.advanceUntilIdle()

        assertEquals(
            ForgotPasswordEffect.NavigateToOtpVerify("user@example.com"),
            viewModel.effect.first()
        )
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `submitting blank email shows required error without network call`() = runTest(dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = ForgotPasswordViewModel(ForgotPasswordUseCase(repository))

        viewModel.onIntent(ForgotPasswordIntent.EmailChanged("   "))
        viewModel.onIntent(ForgotPasswordIntent.Submit)
        testScheduler.advanceUntilIdle()

        assertEquals(
            UiText.Resource(R.string.auth_error_required),
            viewModel.state.value.emailError
        )
    }

    @Test
    fun `server failure surfaces as an effect`() = runTest(dispatcher) {
        val repository = object : FakeAuthRepository() {
            override suspend fun forgotPassword(email: String): Result<Unit> =
                Result.Error(DomainError.ServerError("Failed to send OTP"))
        }
        val viewModel = ForgotPasswordViewModel(ForgotPasswordUseCase(repository))

        viewModel.onIntent(ForgotPasswordIntent.EmailChanged("user@example.com"))
        viewModel.onIntent(ForgotPasswordIntent.Submit)
        testScheduler.advanceUntilIdle()

        assertEquals(
            ForgotPasswordEffect.ShowError(UiText.Dynamic("Failed to send OTP")),
            viewModel.effect.first()
        )
    }
}
