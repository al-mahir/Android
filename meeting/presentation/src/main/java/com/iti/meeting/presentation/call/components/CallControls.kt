package com.iti.meeting.presentation.call.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.designsystem.theme.Theme
import com.iti.meeting.presentation.R
import com.iti.meeting.presentation.call.audio.AudioOutputDevice

/** Palette for the call surface. Deliberately fixed rather than theme-derived: a video call is a
 * dark, chrome-free surface in every app that has one, and it must stay dark in light mode too so
 * the video is the brightest thing on screen. */
object CallColors {
    val background = Color(0xFF101114)
    val tile = Color(0xFF1E2026)
    val scrim = Color(0x66000000)
    val controlBar = Color(0xCC17181C)
    val controlIdle = Color(0x2EFFFFFF)
    val controlActive = Color(0xFFFFFFFF)
    val danger = Color(0xFFE5484D)
    val onDanger = Color(0xFFFFFFFF)
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xB3FFFFFF)
}

/**
 * The in-call control bar.
 *
 * Layout intent: the destructive control (leave) is visually separated and larger, the two controls
 * users reach for constantly (mic, camera) sit closest to the thumbs, and the two that are
 * occasional (output, camera options) open sheets rather than cycling blindly.
 */
@Composable
fun CallControlsBar(
    isMicEnabled: Boolean,
    isCameraEnabled: Boolean,
    audioDevice: AudioOutputDevice,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onOpenAudioOutput: () -> Unit,
    onOpenCameraOptions: () -> Unit,
    onSwitchCamera: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .background(CallColors.controlBar, Theme.shapes.circle)
                .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CallControlButton(
                icon = if (isMicEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                contentDescription = stringResource(
                    if (isMicEnabled) R.string.meeting_call_mic_mute else R.string.meeting_call_mic_unmute
                ),
                isOn = isMicEnabled,
                onClick = onToggleMic,
            )
            CallControlButton(
                icon = if (isCameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                contentDescription = stringResource(
                    if (isCameraEnabled) R.string.meeting_call_camera_turn_off else R.string.meeting_call_camera_turn_on
                ),
                isOn = isCameraEnabled,
                onClick = onToggleCamera,
                onLongClick = onOpenCameraOptions,
            )
            CallControlButton(
                icon = Icons.Filled.Cameraswitch,
                contentDescription = stringResource(R.string.meeting_call_switch_camera),
                isOn = false,
                enabled = isCameraEnabled,
                onClick = onSwitchCamera,
                onLongClick = onOpenCameraOptions,
            )
            CallControlButton(
                icon = audioDevice.icon,
                contentDescription = stringResource(R.string.meeting_call_audio_output),
                isOn = audioDevice != AudioOutputDevice.EARPIECE,
                onClick = onOpenAudioOutput,
            )
            CallControlButton(
                icon = Icons.Filled.CallEnd,
                contentDescription = stringResource(R.string.meeting_call_leave),
                isOn = true,
                containerColor = CallColors.danger,
                contentColor = CallColors.onDanger,
                size = 60.dp,
                onClick = onLeave,
            )
        }
        Spacer(modifier = Modifier.height(Theme.spacing.small))
        Text(
            text = stringResource(audioDevice.labelRes),
            style = Theme.typography.body.small,
            color = CallColors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A single round control.
 *
 * `isOn` follows the convention every call app uses: a *filled white* button means the capability
 * is OFF (it's the thing you'd tap to fix), and a translucent one means it's on and fine. Getting
 * that backwards is the single most common way call UIs confuse people.
 */
@Composable
fun CallControlButton(
    icon: ImageVector,
    contentDescription: String,
    isOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    size: Dp = 54.dp,
) {
    val targetContainer = containerColor ?: if (isOn) CallColors.controlIdle else CallColors.controlActive
    val targetContent = contentColor ?: if (isOn) CallColors.textPrimary else Color.Black
    val container by animateColorAsState(targetContainer, label = "controlContainer")
    val content by animateColorAsState(targetContent, label = "controlContent")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.35f, label = "controlAlpha")

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container.copy(alpha = container.alpha * alpha))
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content.copy(alpha = alpha),
            modifier = Modifier.size(Theme.size.iconMedium),
        )
    }
}

/** A pill showing the call duration, or the reconnecting state when the connection is wobbling. */
@Composable
fun CallStatusPill(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(CallColors.scrim, Theme.shapes.circle)
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        Box(modifier = Modifier.size(Theme.size.statusDot).background(accent, CircleShape))
        Text(text = text, style = Theme.typography.body.small, color = CallColors.textPrimary)
    }
}
