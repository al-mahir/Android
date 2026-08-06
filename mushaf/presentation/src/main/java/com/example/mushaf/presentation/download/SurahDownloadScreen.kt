package com.example.mushaf.presentation.download

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.topbar.PlainTitleTopBar
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.download.state.SurahDownloadIntent
import com.example.mushaf.presentation.download.state.SurahDownloadItem
import com.example.mushaf.presentation.download.state.SurahDownloadUiState
import com.example.mushaf.presentation.download.components.resolve
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import com.example.designsystem.R as DesignSystemR

@Composable
fun SurahDownloadScreen(
    reciterId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SurahDownloadViewModel = koinViewModel(
        parameters = { parametersOf(reciterId) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    com.example.mushaf.presentation.core.mvi.ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is com.example.mushaf.presentation.download.state.SurahDownloadEffect.ShowError -> {
                val message = context.getString(effect.messageRes)
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        containerColor = Theme.colors.backGround,
        modifier = modifier
    ) { padding ->
        SurahDownloadContent(
            state = state,
            onIntent = viewModel::onIntent,
            onBack = onBack,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun SurahDownloadContent(
    state: SurahDownloadUiState,
    onIntent: (SurahDownloadIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
    ) {
        PlainTitleTopBar(
            title = state.reciterName?.resolve() ?: stringResource(R.string.downloads_title_reciters),
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Theme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)
        ) {
            items(state.surahs, key = { it.surahNumber }) { item ->
                SurahDownloadCard(
                    item = item,
                    onDownload = { onIntent(SurahDownloadIntent.DownloadSurah(item.surahNumber)) },
                    onCancel = { onIntent(SurahDownloadIntent.CancelDownload(item.surahNumber)) }
                )
            }
        }
    }
}

private fun Int.toArabicNumerals(): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return toString().map { char ->
        if (char in '0'..'9') arabicDigits[char - '0'] else char
    }.joinToString("")
}

@Composable
fun SurahDownloadCard(
    item: SurahDownloadItem,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isArabic = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "ar"
    val numberStr = if (isArabic) item.surahNumber.toArabicNumerals() else item.surahNumber.toString()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(Theme.colors.surface)
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)
    ) {
        Text(
            text = "$numberStr. ${item.name}",
            style = Theme.typography.body.large.copy(fontWeight = FontWeight.Medium),
            color = Theme.colors.primaryFont,
            modifier = Modifier.weight(1f)
        )

        when {
            item.status?.isCompleted == true -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)
                ) {
                    Icon(
                        painter = painterResource(DesignSystemR.drawable.ic_check),
                        contentDescription = null,
                        tint = Theme.colors.success,
                        modifier = Modifier.size(Theme.size.iconMedium)
                    )
                    Icon(
                        painter = painterResource(DesignSystemR.drawable.ic_trash),
                        contentDescription = stringResource(R.string.downloads_action_delete),
                        tint = Theme.colors.error,
                        modifier = Modifier
                            .size(Theme.size.iconMedium)
                            .clip(CircleShape)
                            .clickable(onClick = onCancel)
                            .padding(2.dp)
                    )
                }
            }
            item.isDownloading -> {
                Box(
                    modifier = Modifier
                        .size(Theme.size.iconMedium)
                        .clip(CircleShape)
                        .clickable(onClick = onCancel)
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = { item.progress / 100f },
                        color = Theme.colors.primary,
                        trackColor = Theme.colors.border,
                        modifier = Modifier.size(Theme.size.iconMedium)
                    )
                    Icon(
                        painter = painterResource(DesignSystemR.drawable.ic_cancel),
                        contentDescription = stringResource(R.string.downloads_action_cancel),
                        tint = Theme.colors.hint,
                        modifier = Modifier.size(Theme.size.iconSmall)
                    )
                }
            }
            else -> {
                Icon(
                    painter = painterResource(DesignSystemR.drawable.ic_download),
                    contentDescription = stringResource(R.string.downloads_action_download),
                    tint = Theme.colors.primary,
                    modifier = Modifier
                        .size(Theme.size.iconMedium)
                        .clip(CircleShape)
                        .clickable(onClick = onDownload)
                        .padding(2.dp)
                )
            }
        }
    }
}
