package com.iti.presentation.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.dialog.ConfirmationDialog
import com.example.designsystem.components.placeholderscreens.StandardEmptyState
import com.example.designsystem.components.search.SearchBar
import com.example.designsystem.components.tabs.TabSelector
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.bookmark.components.AyahBookmarkCard
import com.iti.presentation.bookmark.components.PageBookmarkCard
import com.iti.presentation.bookmark.components.SheikhBookmarkCard
import com.iti.presentation.bookmark.components.SurahBookmarkCard
import com.iti.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun BookmarkScreen(
    onOpenMushafAtPage: (Int) -> Unit,
    onOpenSheikhDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarkViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is BookmarkEffect.OpenMushafAtPage -> onOpenMushafAtPage(effect.page)
            is BookmarkEffect.OpenSheikhDetails -> onOpenSheikhDetails(effect.sheikhId)
        }
    }

    BookmarkContent(
        state = state,
        onTabSelected = { viewModel.onIntent(BookmarkIntent.SelectTab(it)) },
        onSearchChanged = { viewModel.onIntent(BookmarkIntent.SearchQueryChanged(it)) },
        onItemClick = { viewModel.onIntent(BookmarkIntent.OpenBookmark(it)) },
        onRequestRemove = { viewModel.onIntent(BookmarkIntent.RequestRemove(it)) },
        onConfirmRemove = { viewModel.onIntent(BookmarkIntent.ConfirmRemove) },
        onCancelRemove = { viewModel.onIntent(BookmarkIntent.CancelRemove) },
        modifier = modifier,
    )
}

@Composable
private fun BookmarkContent(
    state: BookmarkState,
    onTabSelected: (BookmarkTab) -> Unit,
    onSearchChanged: (String) -> Unit,
    onItemClick: (BookmarkListItem) -> Unit,
    onRequestRemove: (BookmarkListItem) -> Unit,
    onConfirmRemove: () -> Unit,
    onCancelRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
            .padding(bottom = Theme.spacing.medium + Theme.spacing.small + 48.dp, top = Theme.spacing.medium),
    ) {
        Text(
            text = stringResource(R.string.bookmark_screen_title),
            style = Theme.typography.title,
            color = Theme.colors.primaryFont,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )

        SearchBar(
            query = state.searchQuery,
            onQueryChange = onSearchChanged,
            hint = stringResource(R.string.bookmark_search_hint),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Column(modifier = Modifier.padding(top = 16.dp)) {
            val tabs = BookmarkTab.entries
            TabSelector(
                tabs = tabs.map { stringResource(it.labelRes) },
                selectedIndex = tabs.indexOf(state.selectedTab),
                onTabSelected = { onTabSelected(tabs[it]) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        val visibleItems = state.visibleItems

        if (visibleItems.isEmpty() && !state.isLoading) {
            StandardEmptyState(
                title = stringResource(state.selectedTab.emptyTitleRes()),
                description = stringResource(state.selectedTab.emptyDescriptionRes()),
                iconRes = com.example.designsystem.R.drawable.ic_bookmark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(visibleItems, key = { it.bookmarkId }) { item ->
                    BookmarkRow(
                        item = item,
                        onClick = { onItemClick(item) },
                        onRemove = { onRequestRemove(item) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                item { androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    val pendingRemoval = state.pendingRemoval
    if (pendingRemoval != null) {
        ConfirmationDialog(
            title = stringResource(R.string.bookmark_remove_confirm_title),
            message = stringResource(R.string.bookmark_remove_confirm_message),
            confirmLabel = stringResource(R.string.bookmark_remove_confirm_action),
            dismissLabel = stringResource(R.string.bookmark_remove_cancel_action),
            onConfirm = onConfirmRemove,
            onDismiss = onCancelRemove,
            confirmColor = Theme.colors.error,
            confirmContentColor = Theme.colors.onError,
        )
    }
}

@Composable
private fun BookmarkRow(
    item: BookmarkListItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is BookmarkListItem.SurahItem -> SurahBookmarkCard(item, onClick, onRemove, modifier)
        is BookmarkListItem.AyahItem -> AyahBookmarkCard(item, onClick, onRemove, modifier)
        is BookmarkListItem.PageItem -> PageBookmarkCard(item, onClick, onRemove, modifier)
        is BookmarkListItem.SheikhItem -> SheikhBookmarkCard(item, onClick, onRemove, modifier)
    }
}

private fun BookmarkTab.emptyTitleRes(): Int = when (this) {
    BookmarkTab.SURAH -> R.string.bookmark_empty_surah_title
    BookmarkTab.AYAH -> R.string.bookmark_empty_ayah_title
    BookmarkTab.PAGE -> R.string.bookmark_empty_page_title
    BookmarkTab.SHEIKH -> R.string.bookmark_empty_sheikh_title
}

private fun BookmarkTab.emptyDescriptionRes(): Int = when (this) {
    BookmarkTab.SURAH -> R.string.bookmark_empty_surah_description
    BookmarkTab.AYAH -> R.string.bookmark_empty_ayah_description
    BookmarkTab.PAGE -> R.string.bookmark_empty_page_description
    BookmarkTab.SHEIKH -> R.string.bookmark_empty_sheikh_description
}
