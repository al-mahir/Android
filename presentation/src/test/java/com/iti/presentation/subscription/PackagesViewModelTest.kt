package com.iti.presentation.subscription

import com.iti.domain.core.Result
import com.iti.domain.usecase.subscription.GetSubscriptionPackagesUseCase
import com.iti.domain.usecase.subscription.StartFreeTrialUseCase
import com.iti.presentation.subscription.state.PackagesEffect
import com.iti.presentation.subscription.state.PackagesIntent
import com.iti.presentation.testing.FakeAlmahirRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PackagesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `selecting a package only navigates to checkout — it never purchases directly`() = runTest(dispatcher) {
        val repository = FakeAlmahirRepository()
        val viewModel = PackagesViewModel(
            getSubscriptionPackages = GetSubscriptionPackagesUseCase(repository),
            startFreeTrial = StartFreeTrialUseCase(repository),
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(PackagesIntent.SelectPackageClicked("pkg-intensive"))

        assertEquals(PackagesEffect.NavigateToCheckout("pkg-intensive"), viewModel.effect.first())
    }
}
