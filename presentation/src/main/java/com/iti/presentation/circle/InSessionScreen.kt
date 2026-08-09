package com.iti.presentation.circle

import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.dialog.ConfirmationDialog
import com.example.designsystem.theme.Theme
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

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            InSessionEffect.NavigateBack -> onBack()
            InSessionEffect.OpenMushaf -> onOpenMushaf()
            is InSessionEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    InSessionContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
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
                color = SessionOnSurface,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = participant.name,
            style = Theme.typography.body.small,
            color = SessionSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SessionBottomBar(
    isMicMuted: Boolean,
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
                tint = if (isMicMuted) Theme.colors.error else SessionOnSurface,
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
