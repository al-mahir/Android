package com.example.mushaf.presentation.recite

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.designsystem.components.permission.MicPermissionPreprompt
import com.example.mushaf.presentation.R

/**
 * Which microphone dialog the reader is showing, if any.
 *
 * [Preprompt] MUST be shown before the OS permission dialog (TAH-02 / SEC-03): the system
 * dialog offers one irreversible choice, and a second denial is permanent on Android — so the
 * reciter has to understand the reason before it appears, not after.
 */
enum class MicPrompt {
    /** Privacy explainer, shown before the OS dialog is ever requested. */
    Preprompt,

    /** The permission was refused; recovery is a trip to the app's system settings. */
    Denied,

    /** The device refused to open a recorder — typically another app holds the microphone. */
    Unavailable,

    /**
     * The AI service could not be reached, or the session dropped and could not be resumed.
     *
     * Shown rather than swallowed: with live correction unavailable, an empty mistake list means
     * "nothing was checked", which a reciter would otherwise read as "nothing was wrong".
     */
    ServiceUnreachable,
}

fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

/** Opens this app's system settings page, where a denied microphone can be re-granted. */
fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

/**
 * Renders the copy for [prompt] on the shared `:designsystem` dialog.
 *
 * [onConfirm] means different things per prompt and the caller decides: request the OS
 * permission, open system settings, or simply acknowledge.
 */
@Composable
fun MicPromptDialog(
    prompt: MicPrompt,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (prompt) {
        MicPrompt.Preprompt -> MicPermissionPreprompt(
            title = stringResource(R.string.mushaf_mic_preprompt_title),
            message = stringResource(R.string.mushaf_mic_preprompt_message),
            confirmLabel = stringResource(R.string.mushaf_mic_preprompt_confirm),
            dismissLabel = stringResource(R.string.mushaf_mic_preprompt_dismiss),
            reasons = listOf(
                stringResource(R.string.mushaf_mic_preprompt_reason_manual),
                stringResource(R.string.mushaf_mic_preprompt_reason_storage),
                stringResource(R.string.mushaf_mic_preprompt_reason_stop),
            ),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        MicPrompt.Denied -> MicPermissionPreprompt(
            title = stringResource(R.string.mushaf_mic_denied_title),
            message = stringResource(R.string.mushaf_mic_denied_message),
            confirmLabel = stringResource(R.string.mushaf_mic_denied_confirm),
            dismissLabel = stringResource(R.string.mushaf_mic_denied_dismiss),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        MicPrompt.Unavailable -> MicPermissionPreprompt(
            title = stringResource(R.string.mushaf_mic_unavailable_title),
            message = stringResource(R.string.mushaf_mic_unavailable_message),
            confirmLabel = stringResource(R.string.mushaf_mic_unavailable_confirm),
            dismissLabel = stringResource(R.string.mushaf_mic_denied_dismiss),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        MicPrompt.ServiceUnreachable -> MicPermissionPreprompt(
            title = stringResource(R.string.mushaf_live_unreachable_title),
            message = stringResource(R.string.mushaf_live_unreachable_message),
            confirmLabel = stringResource(R.string.mushaf_mic_unavailable_confirm),
            dismissLabel = stringResource(R.string.mushaf_mic_denied_dismiss),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}
