package com.iti.presentation.payment.checkout

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.overlay.animated.OverlayState
import com.example.designsystem.components.overlay.animated.StatusOverlay
import com.example.designsystem.text.asString
import com.example.designsystem.text.resolve
import com.iti.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CheckoutScreen(
    packageId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckoutViewModel = koinViewModel(
        key = packageId,
        parameters = { parametersOf(packageId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            CheckoutEffect.NavigateBack -> onBack()
            is CheckoutEffect.ShowMessage ->
                Toast.makeText(context, effect.message.resolve(context), Toast.LENGTH_SHORT).show()
            is CheckoutEffect.LaunchPaymobSdk -> Unit
        }
    }

    BackHandler(enabled = !state.isProcessing, onBack = onBack)

    CheckoutContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBack,
        modifier = modifier,
    )

    StatusOverlay(
        state = state.overlay.toOverlayState(),
        onDismiss = { viewModel.onIntent(CheckoutIntent.OverlayDismissed) },
    )
}

@Composable
private fun CheckoutOverlay?.toOverlayState(): OverlayState? = when (this) {
    null -> null
    CheckoutOverlay.Loading -> OverlayState.Loading
    is CheckoutOverlay.Success -> OverlayState.Success(message?.asString())
    is CheckoutOverlay.Error -> OverlayState.Error(message?.asString())
}
