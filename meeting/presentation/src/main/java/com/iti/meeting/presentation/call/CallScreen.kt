package com.iti.meeting.presentation.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.WifiCalling3
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.theme.Theme
import com.iti.meeting.presentation.agora.AgoraEngineWrapper
import com.iti.meeting.presentation.agora.AgoraLocalVideo
import com.iti.meeting.presentation.agora.AgoraRemoteVideo
import org.koin.androidx.compose.koinViewModel

private val CallBackground = Color(0xFF121317)
private val SurfaceScrim = Color(0xFF2A2C33)
private val DangerRed = Color(0xFFE94235)

@Composable
fun CallScreen(
    token: String,
    channelName: String,
    uid: Int,
    onLeave: () -> Unit,
    viewModel: CallViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.toggleMic()
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.toggleCamera()
    }
    val joinPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.joinChannel(
            context = context,
            token = token,
            channelName = channelName,
            uid = uid,
            micEnabled = results[Manifest.permission.RECORD_AUDIO] == true,
            cameraEnabled = results[Manifest.permission.CAMERA] == true,
        )
    }

    LaunchedEffect(Unit) {
        joinPermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
    }

    Box(modifier = Modifier.fillMaxSize().background(CallBackground)) {
        when (val current = state) {
            is CallUiState.Connecting -> ConnectingContent()

            is CallUiState.InCall -> InCallContent(
                state = current,
                engine = viewModel.engine,
                onToggleMic = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.toggleMic()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onToggleCamera = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.toggleCamera()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onToggleSpeaker = viewModel::toggleSpeaker,
                onSwitchCamera = viewModel::switchCamera,
                onLeave = onLeave,
            )

            is CallUiState.Error -> ErrorContent(message = current.message, onLeave = onLeave)
        }
    }
}

@Composable
private fun ConnectingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Connecting…", color = Color.White, style = Theme.typography.body.large)
    }
}

@Composable
private fun ErrorContent(message: String, onLeave: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.SignalWifiOff, contentDescription = null, tint = Theme.colors.error, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, color = Color.White, style = Theme.typography.body.large)
        Spacer(modifier = Modifier.height(24.dp))
        IconButton(
            onClick = onLeave,
            modifier = Modifier.size(56.dp).background(DangerRed, CircleShape)
        ) {
            Icon(Icons.Filled.CallEnd, contentDescription = "Leave", tint = Color.White)
        }
    }
}

@Composable
private fun InCallContent(
    state: CallUiState.InCall,
    engine: AgoraEngineWrapper?,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSwitchCamera: () -> Unit,
    onLeave: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Remote tile - fills the screen, Meet-style.
        if (engine != null && state.remoteUid != null && state.isRemoteCameraEnabled) {
            AgoraRemoteVideo(
                engine = engine.engine,
                uid = state.remoteUid,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ParticipantPlaceholder(
                label = if (state.remoteUid == null) "Waiting for the other participant to join…" else "Camera is off",
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (state.remoteUid != null && !state.isRemoteMicEnabled) {
            MutedBadge(modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 110.dp))
        }

        // Local PIP tile - always present, like Meet's "you" tile.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(100.dp, 140.dp)
                .clip(Theme.shapes.medium)
                .background(SurfaceScrim)
        ) {
            if (engine != null && state.isCameraEnabled) {
                AgoraLocalVideo(engine = engine.engine, modifier = Modifier.fillMaxSize())
            } else {
                ParticipantPlaceholder(label = null, avatarSize = 40.dp, modifier = Modifier.fillMaxSize())
            }
            if (!state.isMicEnabled) {
                Icon(
                    Icons.Filled.MicOff,
                    contentDescription = "You are muted",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .size(16.dp)
                )
            }
        }

        CallTopBar(
            durationSeconds = state.callDurationSeconds,
            isReconnecting = state.isReconnecting,
            modifier = Modifier.align(Alignment.TopStart),
        )

        CallControlsBar(
            isMicEnabled = state.isMicEnabled,
            isCameraEnabled = state.isCameraEnabled,
            isSpeakerEnabled = state.isSpeakerEnabled,
            onToggleMic = onToggleMic,
            onToggleCamera = onToggleCamera,
            onToggleSpeaker = onToggleSpeaker,
            onSwitchCamera = onSwitchCamera,
            onLeave = onLeave,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ParticipantPlaceholder(
    label: String?,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 72.dp,
) {
    Column(
        modifier = modifier.background(CallBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(SurfaceScrim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(avatarSize / 2),
            )
        }
        if (label != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.85f),
                style = Theme.typography.body.medium,
            )
        }
    }
}

@Composable
private fun MutedBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(Theme.shapes.large)
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.MicOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CallTopBar(
    durationSeconds: Long,
    isReconnecting: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .padding(16.dp)
            .clip(Theme.shapes.large)
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isReconnecting) {
            Icon(Icons.Filled.WifiCalling3, contentDescription = null, tint = Theme.colors.warning, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reconnecting…", color = Color.White, style = Theme.typography.body.small)
        } else {
            Text(
                text = formatDuration(durationSeconds),
                color = Color.White,
                style = Theme.typography.body.small,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CallControlsBar(
    isMicEnabled: Boolean,
    isCameraEnabled: Boolean,
    isSpeakerEnabled: Boolean,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSwitchCamera: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 20.dp, start = 12.dp, end = 12.dp)
            .fillMaxWidth()
            .clip(Theme.shapes.extraLarge)
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CallControlButton(
            icon = if (isMicEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
            contentDescription = if (isMicEnabled) "Mute microphone" else "Unmute microphone",
            isActive = isMicEnabled,
            onClick = onToggleMic,
        )
        CallControlButton(
            icon = if (isCameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
            contentDescription = if (isCameraEnabled) "Turn camera off" else "Turn camera on",
            isActive = isCameraEnabled,
            onClick = onToggleCamera,
        )
        CallControlButton(
            icon = Icons.Filled.FlipCameraAndroid,
            contentDescription = "Switch camera",
            isActive = true,
            onClick = onSwitchCamera,
        )
        CallControlButton(
            icon = if (isSpeakerEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = if (isSpeakerEnabled) "Switch to earpiece" else "Switch to speaker",
            isActive = isSpeakerEnabled,
            onClick = onToggleSpeaker,
        )
        CallControlButton(
            icon = Icons.Filled.CallEnd,
            contentDescription = "Leave call",
            isActive = true,
            containerColor = DangerRed,
            onClick = onLeave,
        )
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    containerColor: Color? = null,
) {
    val background = containerColor
        ?: if (isActive) Color.White.copy(alpha = 0.16f) else Color.White
    val tint = if (containerColor != null || isActive) Color.White else Color.Black
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .background(background, CircleShape)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
