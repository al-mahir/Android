package com.iti.presentation.auth

import com.example.designsystem.text.UiText
import com.iti.domain.auth.usecase.ResetPasswordUseCase
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import com.iti.presentation.R
import com.iti.presentation.auth.resetpassword.ResetPasswordEffect
import com.iti.presentation.auth.resetpassword.ResetPasswordIntent
import com.iti.presentation.auth.resetpassword.ResetPasswordViewModel
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
class ResetPasswordViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `matching strong passwords successfully resets password and navigates to login`() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = ResetPasswordViewModel(ResetPasswordUseCase(repository))

            viewModel.onIntent(ResetPasswordIntent.InitEmail("user@example.com"))
            viewModel.onIntent(ResetPasswordIntent.NewPasswordChanged("P@ssw0rd123"))
            viewModel.onIntent(ResetPasswordIntent.ConfirmPasswordChanged("P@ssw0rd123"))
            viewModel.onIntent(ResetPasswordIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertEquals(
                ResetPasswordEffect.ShowSuccess(UiText.Resource(R.string.auth_password_reset_success)),
                viewModel.effect.first()
            )
            assertEquals(false, viewModel.state.value.isLoading)
        }

    @Test
    fun `mismatched passwords flag confirmation error without calling use case`() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = ResetPasswordViewModel(ResetPasswordUseCase(repository))

            viewModel.onIntent(ResetPasswordIntent.InitEmail("user@example.com"))
            viewModel.onIntent(ResetPasswordIntent.NewPasswordChanged("P@ssw0rd123"))
            viewModel.onIntent(ResetPasswordIntent.ConfirmPasswordChanged("DifferentPassword123!"))
            viewModel.onIntent(ResetPasswordIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertNull(viewModel.state.value.newPasswordError)
            assertEquals(
                UiText.Resource(R.string.auth_error_password_mismatch),
                viewModel.state.value.confirmPasswordError
            )
        }

    @Test
    fun `weak password flags new password error without calling use case`() =
        runTest(dispatcher) {
            val repository = FakeAuthRepository()
            val viewModel = ResetPasswordViewModel(ResetPasswordUseCase(repository))

            viewModel.onIntent(ResetPasswordIntent.InitEmail("user@example.com"))
            viewModel.onIntent(ResetPasswordIntent.NewPasswordChanged("weak"))
            viewModel.onIntent(ResetPasswordIntent.ConfirmPasswordChanged("weak"))
            viewModel.onIntent(ResetPasswordIntent.Submit)
            testScheduler.advanceUntilIdle()

            assertEquals(
                UiText.Resource(R.string.auth_error_weak_password),
                viewModel.state.value.newPasswordError
            )
        }
}
