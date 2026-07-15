package com.example.designsystem.components.placeholderscreens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.designsystem.R
import com.example.designsystem.components.button.PrimaryButton

/**
 * Full-screen placeholder shown for an empty list/collection where the
 * fetch succeeded but the dataset is genuinely empty. Surface a CTA
 * (e.g. "Create item") via [actionButtonText] + [onActionClick] when the
 * user can act on the empty state from here.
 */
@Composable
fun EmptyDataScreen(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.placeholder_empty_data_title),
    description: String? = stringResource(R.string.placeholder_empty_data_description),
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    EmptyState(
        modifier = modifier,
        iconSlot = { PlaceholderIcon(resId = R.drawable.ic_empty_data) },
        titleSlot = { PlaceholderTitle(title) },
        descriptionSlot = description?.let { { PlaceholderDescription(it) } },
        actionSlot = if (actionButtonText != null && onActionClick != null) {
            { PrimaryButton(caption = actionButtonText, onClick = onActionClick) }
        } else null,
    )
}
