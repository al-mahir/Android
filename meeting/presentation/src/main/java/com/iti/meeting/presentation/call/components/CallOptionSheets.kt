package com.iti.meeting.presentation.call.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.theme.Theme
import com.iti.meeting.presentation.R
import com.iti.meeting.presentation.call.audio.AudioOutputDevice

/**
 * Where the remote audio plays out of. Only lists outputs that are actually connected, so the
 * earpiece/speaker choice is always available and headphones/Bluetooth appear only when present.
 */
@Composable
fun AudioOutputSheet(
    available: List<AudioOutputDevice>,
    selected: AudioOutputDevice,
    onSelect: (AudioOutputDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        SheetTitle(stringResource(R.string.meeting_call_audio_output_title))
        available.forEach { device ->
            OptionRow(
                icon = device.icon,
                label = stringResource(device.labelRes),
                isSelected = device == selected,
                onClick = {
                    onSelect(device)
                    onDismiss()
                },
            )
        }
        Spacer(modifier = Modifier.navigationBarsPadding().height(Theme.spacing.medium))
    }
}

/**
 * Explicit front/back choice plus the torch, rather than the blind "flip to the other one" the
 * control bar offers — useful when a sheikh wants to point the back camera at a page of a Mushaf.
 */
@Composable
fun CameraOptionsSheet(
    isFrontCamera: Boolean,
    isTorchAvailable: Boolean,
    isTorchOn: Boolean,
    onSelectFacing: (front: Boolean) -> Unit,
    onToggleTorch: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        SheetTitle(stringResource(R.string.meeting_call_camera_title))
        OptionRow(
            icon = Icons.Filled.PhotoCamera,
            label = stringResource(R.string.meeting_call_camera_front),
            isSelected = isFrontCamera,
            onClick = {
                onSelectFacing(true)
                onDismiss()
            },
        )
        OptionRow(
            icon = Icons.Filled.Cameraswitch,
            label = stringResource(R.string.meeting_call_camera_back),
            isSelected = !isFrontCamera,
            onClick = {
                onSelectFacing(false)
                onDismiss()
            },
        )
        // The torch is a rear-camera feature; offering it while the front camera is live would be
        // a control that does nothing.
        if (isTorchAvailable && !isFrontCamera) {
            ToggleRow(
                icon = if (isTorchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                label = stringResource(R.string.meeting_call_torch),
                checked = isTorchOn,
                onCheckedChange = { onToggleTorch() },
            )
        }
        Spacer(modifier = Modifier.navigationBarsPadding().height(Theme.spacing.medium))
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = Theme.typography.body.large,
        fontWeight = FontWeight.SemiBold,
        color = Theme.colors.primaryFont,
        modifier = Modifier.padding(top = Theme.spacing.small, bottom = Theme.spacing.medium),
    )
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (isSelected) Theme.colors.primary else Theme.colors.primaryFont
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(if (isSelected) Theme.colors.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = Theme.spacing.medium, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Theme.size.iconMedium))
        Spacer(modifier = Modifier.width(Theme.spacing.medium))
        Text(
            text = label,
            style = Theme.typography.body.medium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = stringResource(R.string.meeting_call_selected),
                tint = Theme.colors.primary,
                modifier = Modifier.size(Theme.size.iconSemiMedium),
            )
        }
    }
    Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = Theme.spacing.medium, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                icon,
                contentDescription = null,
                tint = Theme.colors.primaryFont,
                modifier = Modifier.size(Theme.size.iconMedium),
            )
            Spacer(modifier = Modifier.width(Theme.spacing.medium))
            Text(text = label, style = Theme.typography.body.medium, color = Theme.colors.primaryFont)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Theme.colors.onPrimary,
                checkedTrackColor = Theme.colors.primary,
                uncheckedThumbColor = Theme.colors.hint,
                uncheckedTrackColor = Theme.colors.field,
            ),
        )
    }
}

val AudioOutputDevice.icon: ImageVector
    get() = when (this) {
        AudioOutputDevice.EARPIECE -> Icons.Filled.PhoneInTalk
        AudioOutputDevice.SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
        AudioOutputDevice.WIRED_HEADSET -> Icons.Filled.Headphones
        AudioOutputDevice.BLUETOOTH -> Icons.Filled.Bluetooth
    }

val AudioOutputDevice.labelRes: Int
    get() = when (this) {
        AudioOutputDevice.EARPIECE -> R.string.meeting_call_audio_earpiece
        AudioOutputDevice.SPEAKER -> R.string.meeting_call_audio_speaker
        AudioOutputDevice.WIRED_HEADSET -> R.string.meeting_call_audio_wired
        AudioOutputDevice.BLUETOOTH -> R.string.meeting_call_audio_bluetooth
    }
