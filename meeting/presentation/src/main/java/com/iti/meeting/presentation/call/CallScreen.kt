package com.iti.meeting.presentation.call

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.avatar.InitialsAvatar
import com.example.designsystem.theme.Theme
import com.iti.meeting.presentation.R
import com.iti.meeting.presentation.agora.AgoraEngineWrapper
import com.iti.meeting.presentation.agora.AgoraLocalVideo
import com.iti.meeting.presentation.agora.AgoraRemoteVideo
import com.iti.meeting.presentation.call.audio.AudioOutputDevice
import com.iti.meeting.presentation.call.components.AudioOutputSheet
import com.iti.meeting.presentation.call.components.CallColors
import com.iti.meeting.presentation.call.components.CallControlsBar
import com.iti.meeting.presentation.call.components.CallStatusPill
import com.iti.meeting.presentation.call.components.CameraOptionsSheet
import com.iti.meeting.presentation.call.state.CallErrorReason
import com.iti.meeting.presentation.call.state.CallUiState
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

/**
 * Runs [block] once per [requestId], **synchronously during composition**.
 *
 * The Unit-returning `remember` is deliberate, not an oversight — the reset it performs has to land
 * before the caller first reads `state`. Every lint-approved alternative (`SideEffect`,
 * `LaunchedEffect`) runs *after* composition, which lets one frame of the previous call's terminal
 * `Ended` state through; that single frame is enough for [CallScreen]'s own `state is Ended` watcher
 * to pop a brand-new call's screen straight back out. See the reused-ViewModel entries in
 * `docs/Meeting-Call-Lifecycle-Status.md` for the two field reports this prevents.
 */
@Composable
@Suppress("RememberReturnType")
private fun PrepareForRequest(requestId: String, block: () -> Unit) {
    remember(requestId) { block() }
}

@Composable
fun CallScreen(
    requestId: String,
    token: String,
    channelName: String,
    userAccount: String,
    remoteDisplayName: String? = null,
    onLeave: () -> Unit,
    viewModel: CallViewModel = koinViewModel(),
) {
    PrepareForRequest(requestId) { viewModel.prepareForRequest(requestId) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    android.util.Log.d("MeetingLifecycle", "CallScreen: composed requestId=$requestId state=$state")

    val handleLeave: () -> Unit = {
        android.util.Log.d("MeetingLifecycle", "CallScreen: handleLeave requestId=$requestId")
        viewModel.endCall()
    }

    BackHandler(enabled = state !is CallUiState.Ended) { handleLeave() }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.toggleMic()
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.toggleCamera()
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.selectAudioDevice(AudioOutputDevice.BLUETOOTH)
    }
    val selectAudioDevice: (AudioOutputDevice) -> Unit = { device ->
        val needsBluetoothConsent = device == AudioOutputDevice.BLUETOOTH &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !context.hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        if (needsBluetoothConsent) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            viewModel.selectAudioDevice(device)
        }
    }
    val joinPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.joinChannel(
            context = context,
            requestId = requestId,
            token = token,
            channelName = channelName,
            userAccount = userAccount,
            remoteDisplayName = remoteDisplayName,
            micEnabled = false,
            cameraEnabled = false,
        )
    }

    LaunchedEffect(Unit) {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        joinPermissionsLauncher.launch(permissions.toTypedArray())
    }

    if (state is CallUiState.Ended) {
        LaunchedEffect(Unit) { onLeave() }
    }

    Box(modifier = Modifier.fillMaxSize().background(CallColors.background)) {
        when (val current = state) {
            is CallUiState.Idle, is CallUiState.Connecting ->
                ConnectingContent(remoteDisplayName = remoteDisplayName)

            is CallUiState.InCall -> InCallContent(
                state = current,
                engine = viewModel.engine,
                remoteDisplayName = remoteDisplayName,
                onToggleMic = {
                    if (context.hasPermission(Manifest.permission.RECORD_AUDIO)) {
                        viewModel.toggleMic()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onToggleCamera = {
                    if (context.hasPermission(Manifest.permission.CAMERA)) {
                        viewModel.toggleCamera()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onSelectAudioDevice = selectAudioDevice,
                onSelectCameraFacing = viewModel::setCameraFacing,
                onToggleTorch = viewModel::toggleTorch,
                onSwitchCamera = viewModel::switchCamera,
                onLeave = handleLeave,
            )

            CallUiState.Ended -> ConnectingContent(remoteDisplayName = remoteDisplayName)

            is CallUiState.Error -> ErrorContent(state = current, onLeave = handleLeave)
        }
    }
}

@Composable
private fun ConnectingContent(remoteDisplayName: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Theme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ParticipantAvatar(name = remoteDisplayName, size = 96.dp)
        Spacer(modifier = Modifier.height(Theme.spacing.large))
        Text(
            text = remoteDisplayName ?: stringResource(R.string.meeting_call_participant_generic),
            style = Theme.typography.title,
            color = CallColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(Theme.spacing.medium))
        Text(
            text = stringResource(R.string.meeting_call_connecting),
            style = Theme.typography.body.medium,
            color = CallColors.textSecondary,
        )
        Spacer(modifier = Modifier.height(Theme.spacing.large))
        CircularProgressIndicator(color = Theme.colors.primary, strokeWidth = 3.dp)
    }
}

@Composable
private fun ErrorContent(state: CallUiState.Error, onLeave: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Theme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Theme.colors.error.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.SignalWifiOff,
                contentDescription = null,
                tint = Theme.colors.error,
                modifier = Modifier.size(Theme.size.iconLarge),
            )
        }
        Spacer(modifier = Modifier.height(Theme.spacing.large))
        Text(
            text = stringResource(state.reason.messageRes),
            style = Theme.typography.body.large,
            color = CallColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        state.agoraErrorCode?.let { code ->
            Spacer(modifier = Modifier.height(Theme.spacing.small))
            Text(
                text = stringResource(R.string.meeting_call_error_code, code),
                style = Theme.typography.body.small,
                color = CallColors.textSecondary,
            )
        }
        Spacer(modifier = Modifier.height(Theme.spacing.extraLarge))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(CallColors.danger)
                .clickable(onClick = onLeave),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CallEnd,
                contentDescription = stringResource(R.string.meeting_call_leave),
                tint = CallColors.onDanger,
            )
        }
    }
}

private val CallErrorReason.messageRes: Int
    get() = when (this) {
        CallErrorReason.CONNECT_FAILED -> R.string.meeting_call_error_connect_failed
        CallErrorReason.CONNECTION_LOST -> R.string.meeting_call_error_connection_lost
        CallErrorReason.ENGINE_ERROR -> R.string.meeting_call_error_engine
    }

@Composable
private fun InCallContent(
    state: CallUiState.InCall,
    engine: AgoraEngineWrapper?,
    remoteDisplayName: String?,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onSelectAudioDevice: (AudioOutputDevice) -> Unit,
    onSelectCameraFacing: (Boolean) -> Unit,
    onToggleTorch: () -> Unit,
    onSwitchCamera: () -> Unit,
    onLeave: () -> Unit,
) {
    var showAudioSheet by remember { mutableStateOf(false) }
    var showCameraSheet by remember { mutableStateOf(false) }
    var isPipSwapped by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(controlsVisible, showAudioSheet, showCameraSheet, state.remoteUid) {
        if (controlsVisible && !showAudioSheet && !showCameraSheet && state.remoteUid != null) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    val remoteIsVisible = engine != null && state.remoteUid != null && state.isRemoteCameraEnabled
    val localIsVisible = engine != null && state.isCameraEnabled

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controlsVisible = !controlsVisible }
    ) {
        // Tapping the PiP swaps which stream is on the main stage. Exactly one composable renders
        // each stream at a time, which matters: Agora binds one canvas per stream, so having both
        // tiles render the same stream would leave whichever registered first showing black.
        MainStage(
            showLocal = isPipSwapped,
            engine = engine,
            state = state,
            remoteDisplayName = remoteDisplayName,
            remoteIsVisible = remoteIsVisible,
            localIsVisible = localIsVisible,
        )

        if (state.remoteUid != null && !state.isRemoteMicEnabled && !isPipSwapped) {
            MutedBadge(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = Theme.spacing.large, bottom = 132.dp),
            )
        }

        PipTile(
            showLocal = !isPipSwapped,
            engine = engine,
            state = state,
            remoteDisplayName = remoteDisplayName,
            remoteIsVisible = remoteIsVisible,
            localIsVisible = localIsVisible,
            onClick = { isPipSwapped = !isPipSwapped },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 64.dp, end = Theme.spacing.medium),
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(CHROME_ANIM_MS)) + slideInVertically(tween(CHROME_ANIM_MS)) { -it },
            exit = fadeOut(tween(CHROME_ANIM_MS)) + slideOutVertically(tween(CHROME_ANIM_MS)) { -it },
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            CallTopBar(
                title = remoteDisplayName ?: stringResource(R.string.meeting_call_participant_generic),
                durationSeconds = state.callDurationSeconds,
                isReconnecting = state.isReconnecting,
                isWaitingForRemote = state.remoteUid == null,
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(CHROME_ANIM_MS)) + slideInVertically(tween(CHROME_ANIM_MS)) { it },
            exit = fadeOut(tween(CHROME_ANIM_MS)) + slideOutVertically(tween(CHROME_ANIM_MS)) { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            CallControlsBar(
                isMicEnabled = state.isMicEnabled,
                isCameraEnabled = state.isCameraEnabled,
                audioDevice = state.audioDevice,
                onToggleMic = onToggleMic,
                onToggleCamera = onToggleCamera,
                onOpenAudioOutput = { showAudioSheet = true },
                onOpenCameraOptions = { showCameraSheet = true },
                onSwitchCamera = onSwitchCamera,
                onLeave = onLeave,
            )
        }
    }

    if (showAudioSheet) {
        AudioOutputSheet(
            available = state.availableAudioDevices,
            selected = state.audioDevice,
            onSelect = onSelectAudioDevice,
            onDismiss = { showAudioSheet = false },
        )
    }

    if (showCameraSheet) {
        CameraOptionsSheet(
            isFrontCamera = state.isFrontCamera,
            isTorchAvailable = state.isTorchAvailable,
            isTorchOn = state.isTorchOn,
            onSelectFacing = onSelectCameraFacing,
            onToggleTorch = onToggleTorch,
            onDismiss = { showCameraSheet = false },
        )
    }
}

@Composable
private fun MainStage(
    showLocal: Boolean,
    engine: AgoraEngineWrapper?,
    state: CallUiState.InCall,
    remoteDisplayName: String?,
    remoteIsVisible: Boolean,
    localIsVisible: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            showLocal && localIsVisible ->
                AgoraLocalVideo(engine = engine!!.engine, modifier = Modifier.fillMaxSize())

            showLocal ->
                ParticipantPlaceholder(
                    name = stringResource(R.string.meeting_call_you),
                    label = stringResource(R.string.meeting_call_camera_off),
                    modifier = Modifier.fillMaxSize(),
                )

            remoteIsVisible ->
                AgoraRemoteVideo(
                    engine = engine!!.engine,
                    uid = state.remoteUid!!,
                    modifier = Modifier.fillMaxSize(),
                )

            else ->
                ParticipantPlaceholder(
                    name = remoteDisplayName,
                    label = stringResource(
                        if (state.remoteUid == null) R.string.meeting_call_waiting_for_participant
                        else R.string.meeting_call_camera_off
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}

@Composable
private fun PipTile(
    showLocal: Boolean,
    engine: AgoraEngineWrapper?,
    state: CallUiState.InCall,
    remoteDisplayName: String?,
    remoteIsVisible: Boolean,
    localIsVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 108.dp, height = 152.dp)
            .clip(Theme.shapes.large)
            .background(CallColors.tile)
            .clickable(onClick = onClick),
    ) {
        when {
            showLocal && localIsVisible ->
                AgoraLocalVideo(engine = engine!!.engine, modifier = Modifier.fillMaxSize())

            showLocal ->
                ParticipantPlaceholder(
                    name = stringResource(R.string.meeting_call_you),
                    label = null,
                    avatarSize = 44.dp,
                    modifier = Modifier.fillMaxSize(),
                )

            remoteIsVisible ->
                AgoraRemoteVideo(
                    engine = engine!!.engine,
                    uid = state.remoteUid!!,
                    modifier = Modifier.fillMaxSize(),
                )

            else ->
                ParticipantPlaceholder(
                    name = remoteDisplayName,
                    label = null,
                    avatarSize = 44.dp,
                    modifier = Modifier.fillMaxSize(),
                )
        }

        // The mic indicator always belongs to whoever is in this tile, not always to "you".
        val tileIsMuted = if (showLocal) !state.isMicEnabled else !state.isRemoteMicEnabled
        if (tileIsMuted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Theme.spacing.small)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(CallColors.scrim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MicOff,
                    contentDescription = stringResource(R.string.meeting_call_muted_badge),
                    tint = CallColors.textPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ParticipantPlaceholder(
    name: String?,
    label: String?,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 88.dp,
) {
    Column(
        modifier = modifier.background(CallColors.background).padding(Theme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ParticipantAvatar(name = name, size = avatarSize)
        if (label != null) {
            Spacer(modifier = Modifier.height(Theme.spacing.medium))
            Text(
                text = label,
                color = CallColors.textSecondary,
                style = Theme.typography.body.medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ParticipantAvatar(name: String?, size: Dp) {
    val initials = name?.initials()
    if (initials.isNullOrBlank()) {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(CallColors.tile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = CallColors.textSecondary,
                modifier = Modifier.size(size / 2),
            )
        }
    } else {
        InitialsAvatar(
            initials = initials,
            contentDescription = name,
            containerColor = Theme.colors.primary,
            contentColor = Theme.colors.onPrimary,
            textStyle = Theme.typography.title,
            modifier = Modifier.size(size),
        )
    }
}

@Composable
private fun MutedBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(Theme.shapes.circle)
            .background(CallColors.scrim)
            .padding(horizontal = Theme.spacing.small + Theme.spacing.extraSmall, vertical = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.MicOff,
            contentDescription = null,
            tint = CallColors.textPrimary,
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
        Spacer(modifier = Modifier.width(Theme.spacing.small))
        Text(
            text = stringResource(R.string.meeting_call_muted_badge),
            style = Theme.typography.body.small,
            color = CallColors.textPrimary,
        )
    }
}

@Composable
private fun CallTopBar(
    title: String,
    durationSeconds: Long,
    isReconnecting: Boolean,
    isWaitingForRemote: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = Theme.typography.body.large,
            fontWeight = FontWeight.SemiBold,
            color = CallColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(Theme.spacing.small))
        when {
            isReconnecting -> CallStatusPill(
                text = stringResource(R.string.meeting_call_reconnecting),
                accent = Theme.colors.warning,
            )

            isWaitingForRemote -> CallStatusPill(
                text = stringResource(R.string.meeting_call_connecting),
                accent = Theme.colors.amber,
            )

            else -> CallStatusPill(
                text = formatDuration(durationSeconds),
                accent = Theme.colors.success,
            )
        }
    }
}

private fun android.content.Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** First letters of the first two words — "Ahmed Al Sayed" reads better as "AA" than "AAS". */
private fun String.initials(): String =
    trim().split(WHITESPACE)
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

/** Regular spaces plus NBSP — Arabic names routinely carry the latter. */
private val WHITESPACE = Regex("\\s+")

private const val CONTROLS_AUTO_HIDE_MS = 5_000L
private const val CHROME_ANIM_MS = 220
