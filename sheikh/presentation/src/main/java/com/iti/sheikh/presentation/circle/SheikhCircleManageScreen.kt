package com.iti.sheikh.presentation.circle

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.avatar.InitialsAvatar
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.dialog.ConfirmationDialog
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleMember
import com.iti.meeting.domain.model.circle.CircleMemberRole
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.meeting.domain.model.circle.PendingJoinRequest
import com.iti.sheikh.presentation.R
import com.iti.sheikh.presentation.circle.state.SheikhCircleManageEffect
import com.iti.sheikh.presentation.circle.state.SheikhCircleManageIntent
import com.iti.sheikh.presentation.circle.state.SheikhCircleManageUiState
import com.iti.sheikh.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SheikhCircleManageScreen(
    circleId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SheikhCircleManageViewModel = koinViewModel(parameters = { parametersOf(circleId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            is SheikhCircleManageEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    SheikhCircleManageContent(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.onIntent(SheikhCircleManageIntent.Retry) },
        onApprove = { userId -> viewModel.onIntent(SheikhCircleManageIntent.ApproveRequest(userId)) },
        onReject = { userId -> viewModel.onIntent(SheikhCircleManageIntent.RejectRequest(userId)) },
        onRemove = { userId -> viewModel.onIntent(SheikhCircleManageIntent.RemoveMember(userId)) },
        onStart = { viewModel.onIntent(SheikhCircleManageIntent.StartClicked) },
        onEnd = { viewModel.onIntent(SheikhCircleManageIntent.EndClicked) },
        onCancel = { viewModel.onIntent(SheikhCircleManageIntent.CancelClicked) },
        modifier = modifier,
    )
}

@Composable
private fun SheikhCircleManageContent(
    state: SheikhCircleManageUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onRemove: (String) -> Unit,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingLifecycleAction by remember { mutableStateOf<SheikhCircleManageIntent?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = state.circle?.name ?: stringResource(R.string.sheikh_circle_manage_title),
            onBackClick = onBack,
        )

        when {
            state.isLoading -> ManageSkeleton()
            state.isError || state.circle == null -> NetworkErrorScreen(
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            )
            else -> {
                val circle = state.circle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    CircleSummary(circle = circle)

                    if (state.pendingRequests.isNotEmpty()) {
                        PendingRequestsSection(
                            requests = state.pendingRequests,
                            actionInProgress = state.actionInProgress,
                            onApprove = onApprove,
                            onReject = onReject,
                        )
                    }

                    MembersSection(
                        circle = circle,
                        members = state.members,
                        actionInProgress = state.actionInProgress,
                        onRemove = onRemove,
                    )

                    LifecycleActions(
                        circle = circle,
                        actionInProgress = state.actionInProgress,
                        onStart = onStart,
                        onEnd = { pendingLifecycleAction = SheikhCircleManageIntent.EndClicked },
                        onCancel = { pendingLifecycleAction = SheikhCircleManageIntent.CancelClicked },
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    when (pendingLifecycleAction) {
        SheikhCircleManageIntent.EndClicked -> ConfirmationDialog(
            title = stringResource(R.string.sheikh_circle_end_confirm_title),
            message = stringResource(R.string.sheikh_circle_end_confirm_message),
            confirmLabel = stringResource(R.string.sheikh_circle_end_confirm),
            dismissLabel = stringResource(R.string.sheikh_circle_confirm_dismiss),
            onConfirm = {
                pendingLifecycleAction = null
                onEnd()
            },
            onDismiss = { pendingLifecycleAction = null },
        )
        SheikhCircleManageIntent.CancelClicked -> ConfirmationDialog(
            title = stringResource(R.string.sheikh_circle_cancel_confirm_title),
            message = stringResource(R.string.sheikh_circle_cancel_confirm_message),
            confirmLabel = stringResource(R.string.sheikh_circle_cancel_confirm),
            dismissLabel = stringResource(R.string.sheikh_circle_confirm_dismiss),
            confirmColor = Theme.colors.error,
            confirmContentColor = Theme.colors.onError,
            onConfirm = {
                pendingLifecycleAction = null
                onCancel()
            },
            onDismiss = { pendingLifecycleAction = null },
        )
        null -> Unit
        else -> Unit
    }
}

@Composable
private fun CircleSummary(circle: Circle, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            TypePill(type = circle.type)
            StatusPill(status = circle.status)
        }
        BasicText(
            text = stringResource(
                R.string.sheikh_circle_manage_capacity,
                circle.currentMembers,
                circle.maxParticipants,
            ),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
        )
        BasicText(
            text = stringResource(R.string.sheikh_circle_manage_start_date, circle.startDate),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
        )
    }
}

@Composable
private fun PendingRequestsSection(
    requests: List<PendingJoinRequest>,
    actionInProgress: Boolean,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = stringResource(
                R.string.sheikh_circle_pending_requests_header,
                requests.size,
            ),
            style = Theme.typography.body.large.copy(color = Theme.colors.primaryFont),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            requests.forEach { request ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Theme.colors.surface)
                        .padding(Theme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InitialsAvatar(
                        initials = request.initials,
                        contentDescription = request.displayName,
                        modifier = Modifier.size(Theme.size.avatarMedium),
                    )
                    Spacer(modifier = Modifier.width(Theme.spacing.small))
                    BasicText(
                        text = request.displayName,
                        style = Theme.typography.body.medium.copy(color = Theme.colors.primaryFont),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        caption = stringResource(R.string.sheikh_circle_reject),
                        onClick = { onReject(request.userId) },
                        isDisabled = actionInProgress,
                        height = ButtonHeightCompact,
                        shape = Theme.shapes.medium,
                        modifier = Modifier.width(88.dp),
                    )
                    Spacer(modifier = Modifier.width(Theme.spacing.small))
                    PrimaryButton(
                        caption = stringResource(R.string.sheikh_circle_approve),
                        onClick = { onApprove(request.userId) },
                        isDisabled = actionInProgress,
                        height = ButtonHeightCompact,
                        shape = Theme.shapes.medium,
                        modifier = Modifier.width(88.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MembersSection(
    circle: Circle,
    members: List<CircleMember>,
    actionInProgress: Boolean,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = stringResource(
                R.string.sheikh_circle_members_header,
                members.size,
            ),
            style = Theme.typography.body.large.copy(color = Theme.colors.primaryFont),
        )
        if (members.isEmpty()) {
            BasicText(
                text = stringResource(R.string.sheikh_circle_members_empty),
                style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                members.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Theme.colors.surface)
                            .padding(Theme.spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        InitialsAvatar(
                            initials = member.initials,
                            contentDescription = member.displayName,
                            modifier = Modifier.size(Theme.size.avatarMedium),
                        )
                        Spacer(modifier = Modifier.width(Theme.spacing.small))
                        Column(modifier = Modifier.weight(1f)) {
                            BasicText(
                                text = member.displayName,
                                style = Theme.typography.body.medium.copy(color = Theme.colors.primaryFont),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (member.role == CircleMemberRole.HOST) {
                                BasicText(
                                    text = stringResource(R.string.sheikh_circle_role_host),
                                    style = Theme.typography.body.small.copy(color = Theme.colors.primary),
                                )
                            }
                        }
                        if (member.role != CircleMemberRole.HOST && circle.status == CircleStatus.SCHEDULED) {
                            SecondaryButton(
                                caption = stringResource(R.string.sheikh_circle_remove),
                                onClick = { onRemove(member.userId) },
                                isDisabled = actionInProgress,
                                height = ButtonHeightCompact,
                                shape = Theme.shapes.medium,
                                modifier = Modifier.width(88.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LifecycleActions(
    circle: Circle,
    actionInProgress: Boolean,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
        when (circle.status) {
            CircleStatus.SCHEDULED -> {
                PrimaryButton(
                    caption = stringResource(R.string.sheikh_circle_start),
                    onClick = onStart,
                    isLoading = actionInProgress,
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    caption = stringResource(R.string.sheikh_circle_cancel),
                    onClick = onCancel,
                    isDisabled = actionInProgress,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CircleStatus.ONGOING -> {
                PrimaryButton(
                    caption = stringResource(R.string.sheikh_circle_end),
                    onClick = onEnd,
                    isLoading = actionInProgress,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CircleStatus.COMPLETED, CircleStatus.CANCELLED -> {
                BasicText(
                    text = stringResource(R.string.sheikh_circle_closed_note),
                    style = Theme.typography.body.small.copy(
                        color = Theme.colors.secondaryFont,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TypePill(type: CircleType) {
    val label = when (type) {
        CircleType.PUBLIC -> stringResource(R.string.sheikh_circle_type_public)
        CircleType.PRIVATE -> stringResource(R.string.sheikh_circle_type_private)
    }
    Pill(text = label)
}

@Composable
private fun StatusPill(status: CircleStatus) {
    val label = when (status) {
        CircleStatus.SCHEDULED -> stringResource(R.string.sheikh_circle_status_scheduled)
        CircleStatus.ONGOING -> stringResource(R.string.sheikh_circle_status_ongoing)
        CircleStatus.COMPLETED -> stringResource(R.string.sheikh_circle_status_completed)
        CircleStatus.CANCELLED -> stringResource(R.string.sheikh_circle_status_cancelled)
    }
    Pill(text = label)
}

@Composable
private fun Pill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        BasicText(
            text = text,
            style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
        )
    }
}

@Composable
private fun ManageSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(20.dp)) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Theme.colors.surface),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
