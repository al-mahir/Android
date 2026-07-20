package com.iti.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.settings.state.SettingsEffect
import com.iti.presentation.settings.state.SettingsIntent
import org.koin.androidx.compose.koinViewModel


@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onShowMessage: (Int) -> Unit = {},
    mushafSection: @Composable () -> Unit = {},
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is SettingsEffect.ShowMessage -> onShowMessage(effect.messageRes)
        }
    }

    SettingsContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier.fillMaxSize(),
        mushafSection = mushafSection,
    )
}
