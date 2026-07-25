package com.iti.presentation.circle

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.domain.model.StudyCircle
import com.iti.presentation.R
import com.iti.presentation.circle.state.JoiningCircleEffect
import com.iti.presentation.circle.state.JoiningCircleIntent
import com.iti.presentation.core.mvi.ObserveEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun JoiningCircleScreen(
    circleId: String,
    onBack: () -> Unit,
    onNavigateToSession: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoiningCircleViewModel = koinViewModel(parameters = { parametersOf(circleId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            JoiningCircleEffect.NavigateBack -> onBack()
            is JoiningCircleEffect.NavigateToSession -> onNavigateToSession(effect.circleId)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.joining_circle_title),
            onBackClick = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PulsingWaitIcon()

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.joining_circle_waiting_title),
                style = Theme.typography.title,
                color = Theme.colors.primaryFont,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(
                    R.string.joining_circle_waiting_body,
                    state.circle?.surahName ?: "",
                ),
                style = Theme.typography.body.medium,
                color = Theme.colors.secondaryFont,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            state.circle?.let { circle ->
                CircleInfoCard(circle = circle)
            }

            Spacer(modifier = Modifier.height(40.dp))

            SecondaryButton(
                caption = stringResource(R.string.joining_circle_cancel),
                onClick = { viewModel.onIntent(JoiningCircleIntent.CancelRequest) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PulsingWaitIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(2.dp, Theme.colors.primary.copy(alpha = 0.3f), CircleShape)
            .background(Theme.colors.primary.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.AccessTime,
            contentDescription = null,
            tint = Theme.colors.primary,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun CircleInfoCard(circle: StudyCircle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.colors.surface)
            .padding(16.dp),
    ) {
        Column {
            Text(
                text = circle.surahName,
                style = Theme.typography.body.large,
                color = Theme.colors.primaryFont,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${circle.hostName} · ${circle.participantCount}/${circle.maxParticipants}",
                style = Theme.typography.body.small,
                color = Theme.colors.secondaryFont,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFEBEE))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(text = "LIVE", style = Theme.typography.body.small, color = Color(0xFFE53935))
            }
        }
    }
}
