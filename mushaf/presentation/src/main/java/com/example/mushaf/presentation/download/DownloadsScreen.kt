package com.example.mushaf.presentation.download

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.dialog.ConfirmationDialog
import com.example.designsystem.components.placeholderscreens.EmptyDataScreen
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.topbar.PlainTitleTopBar
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.ResourceKind
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.download.components.DownloadableItemCard
import com.example.mushaf.presentation.download.components.resolve
import com.example.mushaf.presentation.download.state.DownloadsIntent
import com.example.mushaf.presentation.download.state.DownloadsUiState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf




 
@Composable
fun DownloadsScreen(
    kind: ResourceKind,
    onBack: () -> Unit,
    onNavigateToSurahList: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    
    
    val viewModel: DownloadsViewModel = koinViewModel(
        key = kind.name,
        parameters = { parametersOf(kind) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val snackbarHostState = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    com.example.mushaf.presentation.core.mvi.ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is com.example.mushaf.presentation.download.state.DownloadsEffect.ShowMessage -> {
                val message = context.getString(effect.messageRes)
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
            is com.example.mushaf.presentation.download.state.DownloadsEffect.NavigateToSurahList -> {
                onNavigateToSurahList(effect.reciterId)
            }
        }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        containerColor = Theme.colors.backGround,
        modifier = modifier
    ) { padding ->
        DownloadsContent(
            state = state,
            onIntent = viewModel::onIntent,
            onBack = onBack,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun DownloadsContent(
    state: DownloadsUiState,
    onIntent: (DownloadsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        PlainTitleTopBar(
            title = stringResource(state.kind.titleRes()),
            onBackClick = onBack,
        )

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Theme.colors.primary)
            }

            state.errorMessageRes != null -> NetworkErrorScreen(
                modifier = Modifier.fillMaxSize(),
                onRetry = { onIntent(DownloadsIntent.Retry) },
            )

            state.isEmpty -> EmptyDataScreen(modifier = Modifier.fillMaxSize())

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Theme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                items(items = state.resources, key = { it.id }) { resource ->
                    DownloadableItemCard(
                        resource = resource,
                        onDownload = { onIntent(DownloadsIntent.DownloadClicked(resource.id)) },
                        onCancel = { onIntent(DownloadsIntent.CancelClicked(resource.id)) },
                        onDelete = { onIntent(DownloadsIntent.DeleteClicked(resource.id)) },
                        onClick = { onIntent(DownloadsIntent.ItemClicked(resource.id)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item { Box(modifier = Modifier.height(BottomSpacing)) }
            }
        }
    }

    state.pendingDownloadOptions?.let { _ ->
        ConfirmationDialog(
            title = stringResource(R.string.download_dialog_title),
            message = stringResource(R.string.download_dialog_message),
            confirmLabel = stringResource(R.string.download_dialog_entire_quran),
            dismissLabel = stringResource(R.string.download_dialog_select_surahs),
            onConfirm = { onIntent(DownloadsIntent.DownloadOptionsFullQuran) },
            onDismiss = { onIntent(DownloadsIntent.DownloadOptionsSurahs) },
            onDismissRequest = { onIntent(DownloadsIntent.DownloadOptionsDismissed) },
        )
    }

    state.pendingFullDownload?.let { target ->
        val formattedSize = com.example.mushaf.presentation.download.components.rememberFormattedSize(target.sizeBytes)
        ConfirmationDialog(
            title = stringResource(R.string.downloads_full_quran_title),
            message = stringResource(R.string.downloads_full_quran_message, target.name.resolve(), formattedSize),
            confirmLabel = stringResource(R.string.downloads_action_download),
            dismissLabel = stringResource(R.string.downloads_delete_cancel),
            onConfirm = { onIntent(DownloadsIntent.DownloadFullConfirmed) },
            onDismiss = { onIntent(DownloadsIntent.DownloadFullDismissed) },
            confirmColor = Theme.colors.primary,
            confirmContentColor = Theme.colors.onPrimary,
        )
    }

    state.pendingDeletion?.let { target ->
        ConfirmationDialog(
            title = stringResource(R.string.downloads_delete_title),
            message = stringResource(R.string.downloads_delete_message, target.name.resolve()),
            confirmLabel = stringResource(R.string.downloads_delete_confirm),
            dismissLabel = stringResource(R.string.downloads_delete_cancel),
            onConfirm = { onIntent(DownloadsIntent.DeleteConfirmed) },
            onDismiss = { onIntent(DownloadsIntent.DeleteDismissed) },
            confirmColor = Theme.colors.error,
            confirmContentColor = Theme.colors.onError,
        )
    }
}

private fun ResourceKind.titleRes(): Int = when (this) {
    ResourceKind.RECITER -> R.string.downloads_title_reciters
    ResourceKind.TAFSEER -> R.string.downloads_title_tafseers
    ResourceKind.TRANSLATION -> R.string.downloads_title_translations
}

private val BottomSpacing = 24.dp
