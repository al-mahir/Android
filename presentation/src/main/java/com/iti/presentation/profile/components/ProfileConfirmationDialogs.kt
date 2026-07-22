package com.iti.presentation.profile.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.dialog.ConfirmationDialog
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.profile.state.ProfileDialog


@Composable
internal fun ProfileConfirmationDialogs(
    dialog: ProfileDialog?,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (dialog == null) return

    val (titleRes, messageRes, confirmRes) = when (dialog) {
        ProfileDialog.LOGOUT -> Triple(
            R.string.profile_logout_dialog_title,
            R.string.profile_logout_dialog_message,
            R.string.profile_logout_dialog_confirm,
        )

        ProfileDialog.DELETE_ACCOUNT -> Triple(
            R.string.profile_delete_account_dialog_title,
            R.string.profile_delete_account_dialog_message,
            R.string.profile_delete_account_dialog_confirm,
        )
    }

    ConfirmationDialog(
        title = stringResource(titleRes),
        message = stringResource(messageRes),
        confirmLabel = stringResource(confirmRes),
        dismissLabel = stringResource(R.string.profile_dialog_cancel),
        confirmColor = Theme.colors.error,
        confirmContentColor = Theme.colors.onError,
        isConfirmLoading = isProcessing,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
