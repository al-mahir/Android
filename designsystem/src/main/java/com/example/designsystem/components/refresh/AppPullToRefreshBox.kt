package com.example.designsystem.components.refresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme

/**
 * Swipe-to-refresh container styled with the design-system palette — the spinner is drawn in
 * [Theme.colors.primary] on a [Theme.colors.surface] puck, so every screen refreshes with the
 * same affordance instead of the Material default.
 *
 * The gesture rides on nested scroll, so [content] must contain a scrollable (a `LazyColumn`, or
 * a `Modifier.verticalScroll` layout). Non-scrollable full-screen content — an error or empty
 * placeholder — has nothing to dispatch the gesture, so wrap it in [PullToRefreshPlaceholder].
 *
 * @param isRefreshing whether a refresh is currently in flight; drives the spinner.
 * @param onRefresh invoked once per gesture, when the pull crosses the trigger threshold. It is
 *   suppressed while [isRefreshing] is already true, but the owning ViewModel should still guard
 *   re-entrancy — the flag only turns true one recomposition after the gesture fires.
 * @param enabled set false while the screen has nothing to refresh yet (initial skeleton), so a
 *   pull cannot race the very first load.
 */
@Composable
fun AppPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: PullToRefreshState = rememberPullToRefreshState(),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.pullToRefresh(
            isRefreshing = isRefreshing,
            state = state,
            enabled = enabled,
            onRefresh = { if (!isRefreshing) onRefresh() },
        ),
        contentAlignment = contentAlignment,
    ) {
        content()

        // Kept as the last child so it draws above the content, and centred horizontally so the
        // puck lands in the same place in both LTR and RTL.
        PullToRefreshDefaults.Indicator(
            state = state,
            isRefreshing = isRefreshing,
            containerColor = Theme.colors.surface,
            color = Theme.colors.primary,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/**
 * Makes non-scrollable full-screen content pullable inside an [AppPullToRefreshBox].
 *
 * Placeholders (`NetworkErrorScreen`, `EmptyDataScreen`, …) are plain centred columns, so they
 * never dispatch the nested-scroll events the gesture listens for — without this the one screen
 * where a user most wants to pull, the error screen, would ignore them. The content is given a
 * minimum height of the viewport so it still centres exactly as it would unwrapped.
 */
@Composable
fun PullToRefreshPlaceholder(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // An unbounded parent would make `maxHeight` infinite, which `heightIn` cannot take.
        val viewportHeight: Dp = maxHeight.takeIf { it != Dp.Infinity } ?: 0.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight),
                content = content,
            )
        }
    }
}
