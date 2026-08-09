package com.example.designsystem.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.theme.Theme


@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onDismissRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    confirmColor: Color = Theme.colors.primary,
    confirmContentColor: Color = Theme.colors.onPrimary,
    isConfirmLoading: Boolean = false,
) {
    Dialog(
        onDismissRequest = { 
            if (!isConfirmLoading) {
                if (onDismissRequest != null) onDismissRequest() else onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isConfirmLoading,
            dismissOnClickOutside = !isConfirmLoading,
        ),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(Theme.shapes.large)
                .background(Theme.colors.surface)
                .padding(Theme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = title,
                style = Theme.typography.body.large.copy(
                    color = Theme.colors.primaryFont,
                    textAlign = TextAlign.Center,
                ),
            )

            BasicText(
                text = message,
                style = Theme.typography.body.medium.copy(
                    color = Theme.colors.secondaryFont,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(bottom = Theme.spacing.small),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                SecondaryButton(
                    caption = dismissLabel,
                    onClick = onDismiss,
                    isDisabled = isConfirmLoading,
                    height = ButtonHeightCompact,
                    shape = Theme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )

                PrimaryButton(
                    caption = confirmLabel,
                    onClick = onConfirm,
                    isLoading = isConfirmLoading,
                    containerColor = confirmColor,
                    contentColor = confirmContentColor,
                    height = ButtonHeightCompact,
                    shape = Theme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
