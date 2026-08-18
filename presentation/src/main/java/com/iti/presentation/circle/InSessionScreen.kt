package com.iti.presentation.circle

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import com.example.designsystem.components.avatar.InitialsAvatar
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.dialog.ConfirmationDialog
import com.example.designsystem.theme.Theme
import com.iti.meeting.domain.model.circle.PendingJoinRequest
import com.iti.meeting.presentation.call.audio.AudioOutputDevice
import com.iti.meeting.presentation.call.components.AudioOutputSheet
import com.iti.meeting.presentation.call.components.icon
import com.iti.meeting.presentation.call.components.labelRes
import com.iti.meeting.presentation.circle.CircleAudioError
import com.iti.meeting.presentation.circle.CircleAudioStatus
import com.iti.presentation.R
import com.iti.presentation.circle.state.InSessionEffect
import com.iti.presentation.circle.state.InSessionIntent
import com.iti.presentation.circle.state.InSessionUiState
import com.iti.presentation.circle.state.SessionParticipant
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.sheikh.SheikhInitialsAvatar
import com.iti.meeting.domain.model.circle.CircleStatus
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private val SessionBackground = Color(0xFF0F1923)
private val SessionSurface = Color(0xFF1C2A38)
private val SessionOnSurface = Color(0xFFE8EDF2)
private val SessionSecondary = Color(0xFF7A8B9A)
private val SessionSpeakingRing = Color(0xFF4CAF50)

@Composable
fun InSessionScreen(
    circleId: String,
    onBack: () -> Unit,
    onOpenMushaf: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InSessionViewModel = koinViewModel(
        key = circleId,
        parameters = { parametersOf(circleId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        viewModel.onIntent(
            InSessionIntent.MicPermissionResult(granted[Manifest.permission.RECORD_AUDIO] == true)
        )
    }

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            InSessionEffect.NavigateBack -> onBack()
            InSessionEffect.OpenMushaf -> onOpenMushaf()
            is InSessionEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
            InSessionEffect.RequestMicPermission -> {
                // POST_NOTIFICATIONS rides along so the ongoing-session notification can show
                // without a second, separate prompt later.
                val permissions = buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                permissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    LaunchedEffect(circleId) { viewModel.onScreenReady() }

    InSessionContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )

    if (state.isAudioOutputSheetVisible) {
        AudioOutputSheet(
            available = state.availableAudioDevices,
            selected = state.audioDevice,
            onSelect = { viewModel.onIntent(InSessionIntent.SelectAudioDevice(it)) },
            onDismiss = { viewModel.onIntent(InSessionIntent.DismissAudioOutputPicker) },
        )
    }
}

@Composable
private fun InSessionContent(
    state: InSessionUiState,
    onIntent: (InSessionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding( top = Theme.spacing.extraLarge)
            .background(SessionBackground),
    ) {
        BackHandler {
            onIntent(InSessionIntent.Leave)
        }
        
        SessionTopBar(
            surahName = state.circle?.name ?: "",
            isLeaving = state.isLeaving,
            onLeave = { onIntent(InSessionIntent.Leave) },
        )

        AudioStatusBanner(
            status = state.audioStatus,
            isMicPermissionDenied = state.isMicPermissionDenied,
            onRetry = { onIntent(InSessionIntent.RetryAudio) },
        )

        // ── Pending join requests (host only) ─────────────────────────────────
        if (state.isHost && state.pendingRequests.isNotEmpty()) {
            PendingRequestsPanel(
                requests = state.pendingRequests,
                actionInProgress = state.actionInProgress,
                onApprove = { userId -> onIntent(InSessionIntent.ApproveRequest(userId)) },
                onReject = { userId -> onIntent(InSessionIntent.RejectRequest(userId)) },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        HostSection(
            hostName = state.circle?.host?.displayName.orEmpty(),
            hostInitials = state.circle?.host?.initials.orEmpty(),
            hostId = state.circle?.host?.userId.orEmpty(),
        )

        Spacer(modifier = Modifier.weight(1f))

        ParticipantGrid(
            participants = state.participants,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        SessionBottomBar(
            isMicMuted = state.isMicMuted,
            isMicEnabled = state.isMicControlEnabled,
            audioDevice = state.audioDevice,
            isHandRaised = state.isHandRaised,
            unreadChatCount = state.unreadChatCount,
            onIntent = onIntent,
        )
    }

    if (state.isLeaveDialogVisible) {
        val isScheduled = state.circle?.status == CircleStatus.SCHEDULED
        val titleRes = if (state.isHost) {
            if (isScheduled) R.string.circle_cancel_title else R.string.circle_end_title
        } else {
            R.string.circle_leave_title
        }
        val messageRes = if (state.isHost) {
            if (isScheduled) R.string.circle_cancel_message else R.string.circle_end_message
        } else {
            R.string.circle_leave_message
        }
        val confirmRes = if (state.isHost) {
            if (isScheduled) R.string.circle_cancel_confirm else R.string.circle_end_confirm
        } else {
            R.string.circle_leave_confirm
        }
        
        ConfirmationDialog(
            title = stringResource(titleRes),
            message = stringResource(messageRes),
            confirmLabel = stringResource(confirmRes),
            dismissLabel = stringResource(R.string.circle_leave_cancel),
            onConfirm = { onIntent(InSessionIntent.ConfirmLeave) },
            onDismiss = { onIntent(InSessionIntent.DismissLeaveDialog) },
            confirmColor = Theme.colors.error,
            confirmContentColor = Theme.colors.onError,
            isConfirmLoading = state.isLeaving,
        )
    }
}

@Composable
private fun SessionTopBar(
    surahName: String,
    isLeaving: Boolean,
    onLeave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PrimaryButton(
            caption = stringResource(R.string.session_leave),
            onClick = onLeave,
            isLoading = isLeaving,
            height = ButtonHeightCompact,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = surahName,
            style = Theme.typography.body.large,
            color = SessionOnSurface,
        )
        Spacer(modifier = Modifier.width(80.dp))
    }
}

@Composable
private fun HostSection(
    hostName: String,
    hostInitials: String,
    hostId: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(3.dp, SessionSpeakingRing, CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B5E20)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = hostInitials,
                    style = Theme.typography.body.large,
                    color = Color.White,
                )
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(SessionSpeakingRing)
                    .border(2.dp, SessionBackground, CircleShape),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = hostName, style = Theme.typography.body.large, color = SessionOnSurface)
        Text(
            text = stringResource(R.string.session_host_speaking),
            style = Theme.typography.body.small,
            color = SessionSecondary,
        )
    }
}

@Composable
private fun ParticipantGrid(
    participants: List<SessionParticipant>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(participants, key = { it.id }) { participant ->
            ParticipantCell(participant = participant)
        }
    }
}

@Composable
private fun ParticipantCell(participant: SessionParticipant) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SessionSurface)
                    .then(
                        if (participant.isSpeaking) {
                            Modifier.border(2.dp, SessionSpeakingRing, CircleShape)
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = participant.initials,
                    style = Theme.typography.body.medium,
                    // Members on the roster who haven't joined the audio channel are dimmed, so
                    // "listed" and "actually here" are visibly different.
                    color = if (participant.isConnected) SessionOnSurface else SessionSecondary,
                )
            }
            if (participant.isConnected && participant.isMuted) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(SessionSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MicOff,
                        contentDescription = stringResource(R.string.session_mic_off),
                        tint = SessionSecondary,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = participant.name,
            style = Theme.typography.body.small,
            color = SessionSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Explains what the audio transport is doing.
 *
 * Worth the space: before this existed, every audio failure — not started, no permission, failed
 * join — looked identical to a working session with nobody talking.
 */
@Composable
private fun AudioStatusBanner(
    status: CircleAudioStatus,
    isMicPermissionDenied: Boolean,
    onRetry: () -> Unit,
) {
    val message = when {
        isMicPermissionDenied && status is CircleAudioStatus.Live ->
            stringResource(R.string.circle_audio_listen_only)

        status is CircleAudioStatus.Connecting -> stringResource(R.string.circle_audio_connecting)
        status is CircleAudioStatus.NotStarted -> stringResource(R.string.circle_audio_not_started)
        status is CircleAudioStatus.Error -> stringResource(
            when (status.reason) {
                CircleAudioError.CONNECT_FAILED -> R.string.circle_audio_connect_failed
                CircleAudioError.CONNECTION_LOST -> R.string.circle_audio_connection_lost
                CircleAudioError.MIC_PERMISSION_DENIED -> R.string.circle_audio_mic_denied
            }
        )

        else -> null
    } ?: return

    val isRetryable = status is CircleAudioStatus.Error || status is CircleAudioStatus.NotStarted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small)
            .clip(RoundedCornerShape(12.dp))
            .background(SessionSurface)
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        Text(
            text = message,
            style = Theme.typography.body.small,
            color = SessionOnSurface,
            modifier = Modifier.weight(1f),
        )
        if (isRetryable) {
            PrimaryButton(
                caption = stringResource(R.string.circle_audio_retry),
                onClick = onRetry,
                height = ButtonHeightCompact,
                modifier = Modifier.width(96.dp),
            )
        }
    }
}

@Composable
private fun SessionBottomBar(
    isMicMuted: Boolean,
    isMicEnabled: Boolean,
    audioDevice: AudioOutputDevice,
    isHandRaised: Boolean,
    unreadChatCount: Int,
    onIntent: (InSessionIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SessionSurface)
            .navigationBarsPadding()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionBarItem(
            label = stringResource(if (isMicMuted) R.string.session_mic_off else R.string.session_mic_on),
            onClick = { onIntent(InSessionIntent.ToggleMic) },
        ) {
            Icon(
                imageVector = if (isMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = null,
                tint = when {
                    !isMicEnabled -> SessionSecondary
                    isMicMuted -> Theme.colors.error
                    else -> SessionOnSurface
                },
                modifier = Modifier.size(24.dp),
            )
        }

        SessionBarItem(
            label = stringResource(audioDevice.labelRes),
            onClick = { onIntent(InSessionIntent.OpenAudioOutputPicker) },
        ) {
            Icon(
                imageVector = audioDevice.icon,
                contentDescription = null,
                tint = SessionOnSurface,
                modifier = Modifier.size(24.dp),
            )
        }

        SessionBarItem(
            label = stringResource(R.string.session_mushaf),
            onClick = { onIntent(InSessionIntent.OpenMushaf) },
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = SessionOnSurface,
                modifier = Modifier.size(24.dp),
            )
        }

        SessionBarItem(
            label = stringResource(R.string.session_raise_hand),
            onClick = { onIntent(InSessionIntent.ToggleRaiseHand) },
        ) {
            Icon(
                imageVector = Icons.Filled.FrontHand,
                contentDescription = null,
                tint = if (isHandRaised) Theme.colors.primary else SessionOnSurface,
                modifier = Modifier.size(24.dp),
            )
        }

        SessionBarItem(
            label = stringResource(R.string.session_chat),
            onClick = { onIntent(InSessionIntent.OpenChat) },
        ) {
            BadgedBox(
                badge = {
                    if (unreadChatCount > 0) {
                        Badge { Text(text = unreadChatCount.toString()) }
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Chat,
                    contentDescription = null,
                    tint = SessionOnSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun SessionBarItem(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) { content() }
        Text(text = label, style = Theme.typography.body.small, color = SessionSecondary)
    }
}

@Composable
private fun PendingRequestsPanel(
    requests: List<PendingJoinRequest>,
    actionInProgress: Boolean,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround)
            .padding(vertical = Theme.spacing.small),
    ) {
        Text(
            text = stringResource(R.string.in_session_pending_requests, requests.size),
            style = Theme.typography.body.medium,
            color = Theme.colors.secondaryFont,
            modifier = Modifier
                .padding(horizontal = Theme.spacing.medium)
                .padding(bottom = Theme.spacing.small),
        )

        LazyColumn(
            modifier = Modifier.height(160.dp),
        ) {
            items(requests, key = { it.membershipId }) { request ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
                ) {
                    InitialsAvatar(
                        initials = request.initials,
                        imageUrl = request.avatarUrl,
                        contentDescription = request.displayName,
                        modifier = Modifier.size(40.dp),
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.displayName,
                            style = Theme.typography.body.large,
                            color = Theme.colors.primaryFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    PrimaryButton(
                        caption = stringResource(R.string.in_session_approve),
                        onClick = { onApprove(request.userId) },
                        isLoading = actionInProgress,
                        height = ButtonHeightCompact,
                        modifier = Modifier.width(80.dp),
                    )

                    SecondaryButton(
                        caption = stringResource(R.string.in_session_reject),
                        onClick = { onReject(request.userId) },
                        isDisabled = actionInProgress,
                        height = ButtonHeightCompact,
                        modifier = Modifier.width(80.dp),
                    )
                }
            }
        }
    }
}
