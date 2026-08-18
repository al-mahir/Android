package com.example.mushaf.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.R

@Composable
fun AyahNoteDialog(
    initialText: String,
    hasExistingNote: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Theme.shapes.large)
                .background(Theme.colors.surface)
                .padding(Theme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ayah_note_title),
                    style = Theme.typography.body.large.copy(
                        fontWeight = FontWeight.Bold,
                        color = Theme.colors.primaryFont,
                        textAlign = TextAlign.Start,
                    ),
                    modifier = Modifier.weight(1f),
                )
                if (hasExistingNote) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.ayah_note_delete),
                            tint = Theme.colors.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            TextField(
                text = text,
                onTextChange = { text = it },
                hint = stringResource(R.string.ayah_note_hint),
                singleLine = false,
                minLines = 3,
                maxLines = 8,
                fieldHeight = 140.dp,
                fieldVerticalAlignment = Alignment.Top,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                SecondaryButton(
                    caption = stringResource(R.string.ayah_note_cancel),
                    onClick = onDismiss,
                    height = ButtonHeightCompact,
                    shape = Theme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )

                PrimaryButton(
                    caption = stringResource(R.string.ayah_note_save),
                    onClick = { onSave(text) },
                    isDisabled = text.isBlank(),
                    height = ButtonHeightCompact,
                    shape = Theme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
