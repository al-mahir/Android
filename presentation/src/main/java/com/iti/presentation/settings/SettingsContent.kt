package com.iti.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.dialog.ConfirmationDialog
import com.example.designsystem.components.settings.SettingsChevron
import com.example.designsystem.components.settings.SettingsItemRow
import com.example.designsystem.components.settings.SettingsSectionHeader
import com.example.designsystem.components.settings.SettingsSwitch
import com.example.designsystem.components.topbar.PlainTitleTopBar
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import com.iti.domain.settings.model.AppPreferences
import com.iti.presentation.R
import com.iti.presentation.settings.components.LanguageSelectionSheet
import com.iti.presentation.settings.components.ThemeSelectionSheet
import com.iti.presentation.settings.state.SettingsIntent
import com.iti.presentation.settings.state.SettingsSheet
import com.iti.presentation.settings.state.SettingsUiState
import java.util.Locale


@Composable
fun SettingsContent(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    mushafSection: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        PlainTitleTopBar(
            title = stringResource(R.string.settings_title),
            onBackClick = onBack,
        )

        LazyColumn(
            // weight(1f), not fillMaxSize: the list takes exactly what the pinned bar leaves,
            // so it scrolls within that space instead of pushing the bar off-screen.
            modifier = Modifier.weight(1f),
        ) {
            // ---- App appearance ----
            item { SettingsSectionHeader(title = stringResource(R.string.settings_section_appearance)) }

            item {
                SettingsItemRow(
                    title = stringResource(R.string.settings_language),
                    icon = painterResource(com.example.designsystem.R.drawable.ic_globe),
                    onClick = { onIntent(SettingsIntent.LanguageClicked) },
                    trailingContent = { SettingsChevron() },
                )
            }

            item {
                SettingsItemRow(
                    title = stringResource(R.string.settings_theme),
                    icon = painterResource(com.example.designsystem.R.drawable.ic_theme_mode),
                    onClick = { onIntent(SettingsIntent.ThemeClicked) },
                    trailingContent = { SettingsChevron() },
                )
            }

            // ---- Notifications ----
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_notifications))
            }

            item {
                // Switch, not the chevron the mockup shows: this is an in-place toggle with
                // nowhere to navigate to.
                SettingsItemRow(
                    title = stringResource(R.string.settings_reminders),
                    icon = painterResource(com.example.designsystem.R.drawable.ic_notifications),
                    trailingContent = {
                        SettingsSwitch(
                            checked = state.preferences.remindersEnabled,
                            onCheckedChange = { onIntent(SettingsIntent.RemindersToggled(it)) },
                        )
                    },
                )
            }

            // ---- Sounds & haptics ----
            item { SettingsSectionHeader(title = stringResource(R.string.settings_section_sounds)) }

            item {
                SettingsItemRow(
                    title = stringResource(R.string.settings_error_sounds),
                    icon = painterResource(com.example.designsystem.R.drawable.ic_info),
                    trailingContent = {
                        SettingsSwitch(
                            checked = state.preferences.errorSoundsEnabled,
                            onCheckedChange = { onIntent(SettingsIntent.ErrorSoundsToggled(it)) },
                        )
                    },
                )
            }

            // ---- Feature-contributed sections ----
            // Supplied by `:app` from `:mushaf:presentation`. This screen has no idea what is
            // inside, which is exactly why no dependency cycle can form.
            item { mushafSection() }

            // ---- Privacy ----
            item { SettingsSectionHeader(title = stringResource(R.string.settings_section_privacy)) }

            item {
                SettingsItemRow(
                    title = stringResource(R.string.settings_data_usage),
                    icon = painterResource(com.example.designsystem.R.drawable.ic_lock),
                    trailingContent = {
                        SettingsSwitch(
                            checked = state.preferences.dataSaverEnabled,
                            onCheckedChange = { onIntent(SettingsIntent.DataSaverToggled(it)) },
                        )
                    },
                )
            }

            item {
                SettingsItemRow(
                    title = stringResource(R.string.settings_delete_recordings),
                    icon = painterResource(com.example.designsystem.R.drawable.ic_minus_circle),
                    contentColor = Theme.colors.error,
                    iconTint = Theme.colors.error,
                    onClick = { onIntent(SettingsIntent.DeleteRecordingsClicked) },
                    trailingContent = { SettingsChevron() },
                )
            }

            item { AppVersionFooter(version = state.appVersion) }
        }
    }

    when (state.visibleSheet) {
        SettingsSheet.LANGUAGE -> LanguageSelectionSheet(
            selected = state.language,
            onSelect = { onIntent(SettingsIntent.LanguageSelected(it)) },
            onDismiss = { onIntent(SettingsIntent.SheetDismissed) },
        )

        SettingsSheet.THEME -> ThemeSelectionSheet(
            selected = state.themeMode,
            onSelect = { onIntent(SettingsIntent.ThemeSelected(it)) },
            onDismiss = { onIntent(SettingsIntent.SheetDismissed) },
        )

        null -> Unit
    }

    if (state.isDeleteRecordingsDialogVisible) {
        ConfirmationDialog(
            title = stringResource(R.string.settings_delete_recordings_title),
            message = stringResource(R.string.settings_delete_recordings_message),
            confirmLabel = stringResource(R.string.settings_delete_recordings_confirm),
            dismissLabel = stringResource(R.string.settings_delete_recordings_cancel),
            onConfirm = { onIntent(SettingsIntent.DeleteRecordingsConfirmed) },
            onDismiss = { onIntent(SettingsIntent.DeleteRecordingsDismissed) },
            confirmColor = Theme.colors.error,
            confirmContentColor = Theme.colors.onError,
            isConfirmLoading = state.isDeletingRecordings,
        )
    }
}

@Composable
private fun AppVersionFooter(version: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(FooterTopSpacing))

        Text(
            text = stringResource(R.string.settings_version_footer, version),
            style = Theme.typography.hint.small,
            color = Theme.colors.hint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = FooterBottomSpacing),
        )
    }
}

private val FooterTopSpacing: Dp = 24.dp
private val FooterBottomSpacing: Dp = 32.dp

@Preview(name = "Settings RTL", locale = "ar", showBackground = true, heightDp = 900)
@Composable
private fun SettingsContentRtlPreview() {
    // AlMahirTheme already provides LocalLayoutDirection (Rtl for an Arabic locale) along with
    // a locale-aware context, so the preview exercises the same path as the running app.
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        SettingsContent(
            state = SettingsUiState(
                preferences = AppPreferences(),
                isLoading = false,
                appVersion = "1.0.0",
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Settings LTR dark", locale = "en", showBackground = true, heightDp = 900)
@Composable
private fun SettingsContentLtrDarkPreview() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("en")) {
        SettingsContent(
            state = SettingsUiState(
                preferences = AppPreferences(),
                isLoading = false,
                appVersion = "1.0.0",
            ),
            onIntent = {},
            onBack = {},
        )
    }
}
