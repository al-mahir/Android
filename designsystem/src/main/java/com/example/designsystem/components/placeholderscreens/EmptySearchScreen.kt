package com.example.designsystem.components.placeholderscreens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.designsystem.R
import com.example.designsystem.components.button.PrimaryButton

/**
 * Full-screen placeholder shown when a search returns no results. The
 * default description is generic; pass a more specific message (e.g. the
 * query string) when calling. The action slot is opt-in via
 * [actionButtonText] + [onActionClick] for clear-search / change-filter
 * affordances.
 */
@Composable
fun EmptySearchScreen(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.placeholder_empty_search_title),
    description: String? = stringResource(R.string.placeholder_empty_search_description),
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    EmptyState(
        modifier = modifier,
        iconSlot = { PlaceholderIcon(resId = R.drawable.ic_empty_search) },
        titleSlot = { PlaceholderTitle(title) },
        descriptionSlot = description?.let { { PlaceholderDescription(it) } },
        actionSlot = if (actionButtonText != null && onActionClick != null) {
            { PrimaryButton(caption = actionButtonText, onClick = onActionClick) }
        } else null,
    )
}
