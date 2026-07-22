package com.example.designsystem.components.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.example.designsystem.components.bullet.AccentBullet
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.theme.Theme

/**
 * Privacy explainer shown **before** the OS microphone dialog.
 *
 * The ordering is a product requirement, not a nicety (TAH-02 / SEC-03): the system dialog gives
 * the user one irreversible chance to say no, and on Android a second denial is permanent. The
 * reciter has to know *why* the microphone is wanted before that dialog appears.
 *
 * Also used for the denied state, by passing the recovery copy and pointing [onConfirm] at the
 * app's system settings page.
 *
 * @param reasons short bullets explaining what the microphone is used for. Keep to three or
 *   fewer — this is read under a permission prompt, not studied.
 */
@Composable
fun MicPermissionPreprompt(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    reasons: List<String> = emptyList(),
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(Theme.shapes.large)
                .background(Theme.colors.surface)
                .padding(Theme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(Theme.size.iconContainer)
                    .clip(Theme.shapes.circle)
                    .background(Theme.colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    // Decorative: the title and message carry the meaning.
                    contentDescription = null,
                    tint = Theme.colors.onPrimaryContainer,
                    modifier = Modifier.size(Theme.size.iconMedium),
                )
            }

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
            )

            if (reasons.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Theme.spacing.extraSmall),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
                ) {
                    reasons.forEach { reason ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AccentBullet(color = Theme.colors.primary)
                            BasicText(
                                text = reason,
                                style = Theme.typography.body.medium.copy(
                                    color = Theme.colors.secondaryFont,
                                ),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Theme.spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                SecondaryButton(
                    caption = dismissLabel,
                    onClick = onDismiss,
                    height = ButtonHeightCompact,
                    shape = Theme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )

                PrimaryButton(
                    caption = confirmLabel,
                    onClick = onConfirm,
                    height = ButtonHeightCompact,
                    shape = Theme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
