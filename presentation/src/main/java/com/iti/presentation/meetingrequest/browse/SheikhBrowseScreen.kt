package com.iti.presentation.meetingrequest.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.meetingrequest.browse.SheikhBrowseEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun SheikhBrowseScreen(
    onBack: () -> Unit,
    onOpenRequest: (String) -> Unit,
    viewModel: SheikhBrowseViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is SheikhBrowseEffect.NavigateToRequest -> onOpenRequest(effect.sheikhId)
        }
    }

    SheikhBrowseContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
    )
}




