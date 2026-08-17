package com.iti.presentation.subscription

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.subscription.state.PackagesEffect
import com.iti.presentation.subscription.state.PackagesIntent
import org.koin.androidx.compose.koinViewModel

@Composable
fun PackagesScreen(
    onBack: () -> Unit,
    onNavigateToCheckout: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PackagesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is PackagesEffect.NavigateToCheckout -> onNavigateToCheckout(effect.packageId)
            is PackagesEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    PackagesContent(
        state = state,
        onBackClick = onBack,
        onRetryClick = { viewModel.onIntent(PackagesIntent.Retry) },
        onSelectPackageClick = { packageId ->
            viewModel.onIntent(PackagesIntent.SelectPackageClicked(packageId))
        },
        modifier = modifier,
    )
}
