package com.iti.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.theme.Theme
import com.iti.domain.settings.model.AppLanguage
import com.iti.domain.settings.model.ThemeMode
import com.iti.presentation.R


@Composable
private fun <T> SelectionSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = title,
            style = Theme.typography.body.large.copy(fontWeight = FontWeight.SemiBold),
            color = Theme.colors.primaryFont,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = Theme.spacing.medium),
        )

        options.forEach { option ->
            val isSelected = option == selected

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Theme.shapes.medium)
                    .clickable { onSelect(option) }
                    .semantics { role = Role.RadioButton }
                    .background(
                        if (isSelected) Theme.colors.field else Theme.colors.backGround,
                    )
                    .padding(Theme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label(option),
                    style = Theme.typography.body.large,
                    color = if (isSelected) Theme.colors.primary else Theme.colors.primaryFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (isSelected) {
                    Icon(
                        painter = painterResource(com.example.designsystem.R.drawable.ic_check),
                        contentDescription = null,
                        tint = Theme.colors.primary,
                        modifier = Modifier.size(Theme.size.iconSemiMedium),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))
        }

        Spacer(modifier = Modifier.height(SheetBottomSpacing))
    }
}

@Composable
fun LanguageSelectionSheet(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    SelectionSheet(
        title = stringResource(R.string.settings_language),
        options = AppLanguage.entries,
        selected = selected,
        label = { language ->
            stringResource(
                when (language) {
                    AppLanguage.ARABIC -> R.string.settings_language_arabic
                    AppLanguage.ENGLISH -> R.string.settings_language_english
                },
            )
        },
        onSelect = onSelect,
        onDismiss = onDismiss,
    )
}

@Composable
fun ThemeSelectionSheet(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    SelectionSheet(
        title = stringResource(R.string.settings_theme),
        options = ThemeMode.entries,
        selected = selected,
        label = { mode ->
            stringResource(
                when (mode) {
                    ThemeMode.LIGHT -> R.string.settings_theme_light
                    ThemeMode.DARK -> R.string.settings_theme_dark
                    ThemeMode.SYSTEM -> R.string.settings_theme_system
                },
            )
        },
        onSelect = onSelect,
        onDismiss = onDismiss,
    )
}

private val SheetBottomSpacing: Dp = 24.dp
