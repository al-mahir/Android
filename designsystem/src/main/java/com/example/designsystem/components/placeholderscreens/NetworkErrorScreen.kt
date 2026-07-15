package com.example.designsystem.components.placeholderscreens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.designsystem.R
import com.example.designsystem.components.button.PrimaryButton

/**
 * Full-screen placeholder shown when a network request fails. Pass
 * [onRetry] to expose a Retry button — leave it `null` to hide the action
 * (e.g. when the caller wants to handle retry via pull-to-refresh).
 */
@Composable
fun NetworkErrorScreen(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.placeholder_network_error_title),
    description: String? = stringResource(R.string.placeholder_network_error_description),
    actionButtonText: String = stringResource(R.string.placeholder_network_error_action),
    onRetry: (() -> Unit)? = null,
) {
    EmptyState(
        modifier = modifier,
        iconSlot = { PlaceholderIcon(resId = R.drawable.ic_network_error) },
        titleSlot = { PlaceholderTitle(title) },
        descriptionSlot = description?.let { { PlaceholderDescription(it) } },
        actionSlot = onRetry?.let {
            { PrimaryButton(caption = actionButtonText, onClick = it) }
        },
    )
}
