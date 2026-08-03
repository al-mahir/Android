package com.iti.presentation.circle

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.meeting.domain.model.circle.Circle
import com.iti.meeting.domain.model.circle.CircleStatus
import com.iti.meeting.domain.model.circle.CircleType
import com.iti.presentation.R
import com.iti.presentation.circle.state.CircleDetailsEffect
import com.iti.presentation.circle.state.CircleDetailsIntent
import com.iti.presentation.circle.state.CircleDetailsUiState
import com.iti.presentation.circle.state.CircleJoinUiState
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.sheikh.SheikhInitialsAvatar
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CircleDetailsScreen(
    circleId: String,
    onBack: () -> Unit,
    onOpenJoining: (String, String) -> Unit,
    onOpenSession: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CircleDetailsViewModel = koinViewModel(parameters = { parametersOf(circleId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            CircleDetailsEffect.NavigateBack -> onBack()
            is CircleDetailsEffect.OpenJoining -> onOpenJoining(effect.circleId, effect.membershipId)
            is CircleDetailsEffect.OpenSession -> onOpenSession(effect.circleId)
            is CircleDetailsEffect.ShowMessage ->
                Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    CircleDetailsContent(
        state = state,
        onBack = onBack,
        onJoinClicked = { viewModel.onIntent(CircleDetailsIntent.JoinClicked) },
        onPasswordChanged = { viewModel.onIntent(CircleDetailsIntent.PasswordChanged(it)) },
        onSubmitJoin = { viewModel.onIntent(CircleDetailsIntent.SubmitJoin) },
        onDismissPasswordPrompt = { viewModel.onIntent(CircleDetailsIntent.DismissPasswordPrompt) },
        onRetry = { viewModel.onIntent(CircleDetailsIntent.Retry) },
        modifier = modifier,
    )
}

@Composable
private fun CircleDetailsContent(
    state: CircleDetailsUiState,
    onBack: () -> Unit,
    onJoinClicked: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmitJoin: () -> Unit,
    onDismissPasswordPrompt: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = state.circle?.name ?: stringResource(R.string.circle_details_title),
            onBackClick = onBack,
        )

        when {
            state.isLoading -> CircleDetailsSkeleton()
            state.isError || state.circle == null -> NetworkErrorScreen(
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            )
            else -> CircleDetailsBody(
                state = state,
                onJoinClicked = onJoinClicked,
                onPasswordChanged = onPasswordChanged,
                onSubmitJoin = onSubmitJoin,
                onDismissPasswordPrompt = onDismissPasswordPrompt,
            )
        }
    }
}

@Composable
private fun CircleDetailsBody(
    state: CircleDetailsUiState,
    onJoinClicked: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmitJoin: () -> Unit,
    onDismissPasswordPrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val circle = state.circle ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SheikhInitialsAvatar(
                initials = circle.host?.initials.orEmpty(),
                sheikhId = circle.host?.userId.orEmpty(),
                size = 56,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circle.host?.displayName.orEmpty(),
                    style = Theme.typography.body.large,
                    color = Theme.colors.primaryFont,
                )
                Text(
                    text = stringResource(
                        R.string.circle_participants_label,
                        circle.currentMembers,
                        circle.maxParticipants,
                    ),
                    style = Theme.typography.body.small,
                    color = Theme.colors.secondaryFont,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            TypePill(type = circle.type)
            StatusPill(status = circle.status)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.circle_details_date, circle.startDate),
            style = Theme.typography.body.medium,
            color = Theme.colors.secondaryFont,
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (state.joinState) {
            is CircleJoinUiState.PendingApproval -> WaitingChip()
            else -> {
                PrimaryButton(
                    caption = stringResource(R.string.circle_join),
                    onClick = onJoinClicked,
                    isLoading = state.joinState is CircleJoinUiState.Joining,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.passwordPromptVisible) {
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                text = state.password,
                onTextChange = onPasswordChanged,
                hint = stringResource(R.string.circle_password_hint),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(
                caption = stringResource(R.string.circle_password_confirm),
                onClick = onSubmitJoin,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.circle_password_dismiss),
                style = Theme.typography.body.small,
                color = Theme.colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Theme.shapes.small)
                    .clickable(onClick = onDismissPasswordPrompt)
                    .padding(vertical = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TypePill(type: CircleType) {
    val label = when (type) {
        CircleType.PUBLIC -> stringResource(R.string.circle_type_public)
        CircleType.PRIVATE -> stringResource(R.string.circle_type_private)
    }
    Pill(text = label)
}

@Composable
private fun StatusPill(status: CircleStatus) {
    val label = when (status) {
        CircleStatus.SCHEDULED -> stringResource(R.string.circle_status_scheduled)
        CircleStatus.ONGOING -> stringResource(R.string.circle_status_ongoing)
        CircleStatus.COMPLETED -> stringResource(R.string.circle_status_completed)
        CircleStatus.CANCELLED -> stringResource(R.string.circle_status_cancelled)
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
        Text(text = text, style = Theme.typography.body.small, color = Theme.colors.secondaryFont)
    }
}

@Composable
private fun WaitingChip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.medium)
            .background(Theme.colors.primary.copy(alpha = 0.08f))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.circle_waiting_approval),
            style = Theme.typography.body.medium,
            color = Theme.colors.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CircleDetailsSkeleton(modifier: Modifier = Modifier) {
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
