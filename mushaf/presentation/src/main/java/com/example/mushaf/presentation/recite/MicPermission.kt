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







 
enum class MicPrompt {
     
    Preprompt,

     
    Denied,

     
    Unavailable,

    




 
    ServiceUnreachable,
}

fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

 
fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}






 
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
