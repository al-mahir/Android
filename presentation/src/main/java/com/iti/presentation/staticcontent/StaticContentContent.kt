package com.iti.presentation.staticcontent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.loading.ShimmerBox
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.staticcontent.components.MarkdownBlockText
import com.iti.presentation.staticcontent.components.parseMarkdown
import com.iti.presentation.staticcontent.state.StaticContentUiState


@Composable
fun StaticContentContent(
    state: StaticContentUiState,
    fallbackTitle: String,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = state.document?.title ?: fallbackTitle,
            onBackClick = onBackClick,
        )

        when {
            state.hasError -> NetworkErrorScreen(
                modifier = Modifier.fillMaxSize(),
                description = stringResource(
                    state.errorMessageRes ?: R.string.static_content_error_generic
                ),
                onRetry = onRetryClick,
            )

            state.document == null -> StaticContentSkeleton()

            else -> {
                val document = state.document
                // Re-parsed only when the body actually changes, not on every recomposition.
                val blocks = remember(document.body) { parseMarkdown(document.body) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = Theme.spacing.medium,
                        vertical = Theme.spacing.large,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                ) {
                    items(items = blocks) { block ->
                        MarkdownBlockText(block = block)
                    }
                }
            }
        }
    }
}

@Composable
private fun StaticContentSkeleton(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.large),
    ) {
        ShimmerBox(Modifier.width(160.dp).height(26.dp).clip(Theme.shapes.small))
        repeat(8) {
            ShimmerBox(Modifier.fillMaxWidth().height(14.dp).clip(Theme.shapes.small))
        }
    }
}
