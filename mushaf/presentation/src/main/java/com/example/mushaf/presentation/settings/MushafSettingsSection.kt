package com.example.mushaf.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.settings.SettingsChevron
import com.example.designsystem.components.settings.SettingsItemRow
import com.example.designsystem.components.settings.SettingsSectionHeader
import com.example.designsystem.components.settings.SettingsSwitch
import com.example.mushaf.domain.model.ResourceKind
import com.example.mushaf.presentation.R
import org.koin.androidx.compose.koinViewModel
import com.example.designsystem.R as DesignSystemR


@Composable
fun MushafSettingsSection(
    onOpenDownloads: (ResourceKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MushafSettingsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    MushafSettingsSectionContent(
        state = state,
        onIntent = viewModel::onIntent,
        onOpenDownloads = onOpenDownloads,
        modifier = modifier,
    )
}

@Composable
fun MushafSettingsSectionContent(
    state: MushafSettingsUiState,
    onIntent: (MushafSettingsIntent) -> Unit,
    onOpenDownloads: (ResourceKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        
        SettingsSectionHeader(title = stringResource(R.string.settings_section_mushaf))

        SettingsItemRow(
            title = stringResource(R.string.mushaf_tajweed_label),
            subtitle = stringResource(R.string.settings_tajweed_description),
            icon = painterResource(DesignSystemR.drawable.ic_tajweed),
            trailingContent = {
                SettingsSwitch(
                    checked = state.isTajweedEnabled,
                    onCheckedChange = { onIntent(MushafSettingsIntent.TajweedToggled(it)) },
                )
            },
        )

        SettingsSectionHeader(title = stringResource(R.string.settings_section_downloads))

        SettingsItemRow(
            title = stringResource(R.string.downloads_title_reciters),
            icon = painterResource(DesignSystemR.drawable.ic_headphones),
            onClick = { onOpenDownloads(ResourceKind.RECITER) },
            trailingContent = { SettingsChevron() },
        )

        SettingsItemRow(
            title = stringResource(R.string.downloads_title_translations),
            icon = painterResource(DesignSystemR.drawable.ic_translate),
            onClick = { onOpenDownloads(ResourceKind.TRANSLATION) },
            trailingContent = { SettingsChevron() },
        )

        SettingsItemRow(
            title = stringResource(R.string.downloads_title_tafseers),
            icon = painterResource(DesignSystemR.drawable.ic_book_open),
            onClick = { onOpenDownloads(ResourceKind.TAFSEER) },
            trailingContent = { SettingsChevron() },
        )
    }
}
