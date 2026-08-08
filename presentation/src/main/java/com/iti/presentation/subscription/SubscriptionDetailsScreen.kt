package com.iti.presentation.subscription

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.subscription.state.SubscriptionDetailsEffect
import com.iti.presentation.subscription.state.SubscriptionDetailsIntent
import org.koin.androidx.compose.koinViewModel

@Composable
fun SubscriptionDetailsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionDetailsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            SubscriptionDetailsEffect.NavigateBack -> onBack()
            is SubscriptionDetailsEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    SubscriptionDetailsContent(
        state = state,
        onBackClick = { viewModel.onIntent(SubscriptionDetailsIntent.BackClicked) },
        onRetryClick = { viewModel.onIntent(SubscriptionDetailsIntent.Retry) },
        onReturnSubscriptionClick = {
            viewModel.onIntent(SubscriptionDetailsIntent.ReturnSubscriptionClicked)
        },
        onReturnSheetDismiss = { viewModel.onIntent(SubscriptionDetailsIntent.ReturnSheetDismissed) },
        onCancellationMessageChange = { message ->
            viewModel.onIntent(SubscriptionDetailsIntent.CancellationMessageChanged(message))
        },
        onSendCancellationMessageClick = {
            viewModel.onIntent(SubscriptionDetailsIntent.SendCancellationMessageClicked)
        },
        modifier = modifier,
    )
}
