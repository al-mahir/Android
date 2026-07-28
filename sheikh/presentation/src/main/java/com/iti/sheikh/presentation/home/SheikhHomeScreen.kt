package com.iti.sheikh.presentation.home

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iti.sheikh.presentation.core.mvi.ObserveEffect
import com.iti.sheikh.presentation.home.state.SheikhHomeEffect
import com.iti.sheikh.presentation.home.state.SheikhHomeIntent
import org.koin.androidx.compose.koinViewModel

@Composable
fun SheikhHomeScreen(
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SheikhHomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            SheikhHomeEffect.OpenProfile -> onOpenProfile()
            is SheikhHomeEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    SheikhHomeContent(
        state = state,
        onProfileClick = { viewModel.onIntent(SheikhHomeIntent.ProfileClicked) },
        onAvailabilityToggle = { isAvailable ->
            viewModel.onIntent(SheikhHomeIntent.AvailabilityToggled(isAvailable))
        },
        onRetryClick = { viewModel.onIntent(SheikhHomeIntent.Retry) },
        modifier = modifier,
    )
}
