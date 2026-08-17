package com.iti.presentation.meetingrequest.request

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.theme.Theme
import com.iti.domain.model.MeetingEligibility
import com.iti.presentation.R
import kotlinx.coroutines.delay

private val HeroCircleSize = 88.dp
private val StateTransition: ContentTransform =
    (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.92f)) togetherWith
        (fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.92f))

@Composable
fun MeetingRequestContent(
    state: RequestUiState,
    onSend: (String?) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    onCancelExisting: () -> Unit = {},
    onBuyPackage: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
            .padding(Theme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { StateTransition },
            label = "meetingRequestState",
            modifier = Modifier.fillMaxWidth(),
        ) { targetState ->
            when (targetState) {
                RequestUiState.Idle -> IdleContent(onSend = onSend)
                RequestUiState.CheckingQuota -> CheckingQuotaContent()
                is RequestUiState.QuotaBlocked -> QuotaBlockedContent(
                    state = targetState,
                    onBuyPackage = onBuyPackage,
                    onBack = onBack,
                )
                RequestUiState.Sending -> SendingContent()
                is RequestUiState.Pending -> PendingContent(state = targetState, onCancel = onCancel)
                is RequestUiState.Accepted -> AcceptedContent()
                is RequestUiState.Declined -> DeclinedContent(state = targetState, onBack = onBack)
                RequestUiState.Expired -> ExpiredContent(onBack = onBack)
                RequestUiState.Ended -> EndedContent(onBack = onBack)
                is RequestUiState.AlreadyPending -> AlreadyPendingContent(
                    state = targetState,
                    onCancelExisting = onCancelExisting,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun IdleContent(onSend: (String?) -> Unit) {
    var note by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroCircle(
            iconRes = com.example.designsystem.R.drawable.ic_chat,
            containerColor = Theme.colors.primaryContainer,
            iconTint = Theme.colors.primary,
        )
        VerticalSpace(Theme.spacing.medium)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_idle_title),
            style = Theme.typography.title.copy(color = Theme.colors.primaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.small)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_idle_subtitle),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.large)
        TextField(
            text = note,
            onTextChange = { note = it },
            title = stringResource(R.string.meetingrequest_request_note_label),
            hint = stringResource(R.string.meetingrequest_request_note_hint),
            fieldHeight = 96.dp,
            fieldVerticalAlignment = Alignment.Top,
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpace(Theme.spacing.large)
        PrimaryButton(
            caption = stringResource(R.string.meetingrequest_request_send),
            onClick = { onSend(note.ifBlank { null }) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CheckingQuotaContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Theme.colors.primary)
        VerticalSpace(Theme.spacing.medium)
        BasicText(
            text = stringResource(R.string.meetingrequest_quota_checking),
            style = Theme.typography.body.large.copy(color = Theme.colors.primaryFont, textAlign = TextAlign.Center),
        )
    }
}

/**
 * Terminal-but-recoverable state: the student has no usable minutes. Each reason gets its own
 * copy because "buy your first package", "renew an expired one" and "you're a few minutes short"
 * are different problems, and a single generic message would leave the student guessing.
 */
@Composable
private fun QuotaBlockedContent(
    state: RequestUiState.QuotaBlocked,
    onBuyPackage: () -> Unit,
    onBack: () -> Unit,
) {
    val title = when (state.reason) {
        is MeetingEligibility.NoSubscription -> stringResource(R.string.meetingrequest_quota_none_title)
        is MeetingEligibility.Expired -> stringResource(R.string.meetingrequest_quota_expired_title)
        is MeetingEligibility.NoMinutesLeft -> stringResource(R.string.meetingrequest_quota_depleted_title)
        is MeetingEligibility.NotEnoughMinutes -> stringResource(R.string.meetingrequest_quota_low_title)
        else -> stringResource(R.string.meetingrequest_quota_none_title)
    }
    val subtitle = when (val reason = state.reason) {
        is MeetingEligibility.NoSubscription ->
            stringResource(R.string.meetingrequest_quota_none_subtitle)
        is MeetingEligibility.Expired -> reason.packageName?.let {
            stringResource(R.string.meetingrequest_quota_expired_subtitle_named, it)
        } ?: stringResource(R.string.meetingrequest_quota_expired_subtitle)
        is MeetingEligibility.NoMinutesLeft ->
            stringResource(R.string.meetingrequest_quota_depleted_subtitle)
        is MeetingEligibility.NotEnoughMinutes -> stringResource(
            R.string.meetingrequest_quota_low_subtitle,
            reason.remainingMinutes,
            reason.requiredMinutes,
        )
        else -> stringResource(R.string.meetingrequest_quota_none_subtitle)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroCircle(
            iconRes = com.example.designsystem.R.drawable.ic_lock,
            containerColor = Theme.colors.amber.copy(alpha = 0.14f),
            iconTint = Theme.colors.amber,
        )
        VerticalSpace(Theme.spacing.medium)
        BasicText(
            text = title,
            style = Theme.typography.title.copy(color = Theme.colors.primaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.small)
        BasicText(
            text = subtitle,
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.large)
        PrimaryButton(
            caption = stringResource(R.string.meetingrequest_quota_browse_packages),
            onClick = onBuyPackage,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpace(Theme.spacing.small)
        SecondaryButton(
            caption = stringResource(R.string.meetingrequest_request_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SendingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Theme.colors.primary)
        VerticalSpace(Theme.spacing.medium)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_sending_title),
            style = Theme.typography.body.large.copy(color = Theme.colors.primaryFont, textAlign = TextAlign.Center),
        )
    }
}


private fun parseExpiresAt(expiresAtString: String): java.time.Instant {
    val fixed = if (expiresAtString.endsWith("Z") || expiresAtString.contains("+")) {
        expiresAtString
    } else {
        "${expiresAtString}Z"
    }
    return runCatching { java.time.Instant.parse(fixed) }.getOrNull()
        ?: java.time.Instant.now().plusSeconds(60)
}

@Composable
private fun PendingContent(state: RequestUiState.Pending, onCancel: () -> Unit) {
    val totalSeconds = remember(state.expiresAt) {
        val parsed = parseExpiresAt(state.expiresAt)
        (parsed.epochSecond - java.time.Instant.now().epochSecond).coerceAtLeast(1)
    }
    var remainingSeconds by remember(state.expiresAt) {
        val parsed = parseExpiresAt(state.expiresAt)
        mutableLongStateOf((parsed.epochSecond - java.time.Instant.now().epochSecond).coerceAtLeast(0))
    }
    LaunchedEffect(state.expiresAt) {
        val parsed = parseExpiresAt(state.expiresAt)
        while (remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds = (parsed.epochSecond - java.time.Instant.now().epochSecond).coerceAtLeast(0)
        }
    }
    val fraction = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val urgencyColor by animateColorAsState(
        targetValue = when {
            fraction > 0.5f -> Theme.colors.primary
            fraction > 0.2f -> Theme.colors.amber
            else -> Theme.colors.error
        },
        animationSpec = tween(400),
        label = "pendingUrgencyColor",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PulsingCountdownRing(fraction = fraction, color = urgencyColor) {
            Image(
                painter = painterResource(com.example.designsystem.R.drawable.clock_icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(urgencyColor),
                modifier = Modifier.size(Theme.size.iconLarge),
            )
        }
        VerticalSpace(Theme.spacing.medium)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_pending_title),
            style = Theme.typography.title.copy(color = Theme.colors.primaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.small)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_pending_subtitle),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.small)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_pending_expires_in_seconds, remainingSeconds),
            style = Theme.typography.body.small.copy(color = urgencyColor, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.large)
        SecondaryButton(
            caption = stringResource(R.string.meetingrequest_request_cancel),
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AcceptedContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroCircle(
            iconRes = com.example.designsystem.R.drawable.ic_check,
            containerColor = Theme.colors.success.copy(alpha = 0.16f),
            iconTint = Theme.colors.success,
        )
        VerticalSpace(Theme.spacing.medium)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_accepted_title),
            style = Theme.typography.title.copy(color = Theme.colors.primaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.small)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_accepted_subtitle),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.medium)
        CircularProgressIndicator(color = Theme.colors.success)
    }
}

@Composable
private fun DeclinedContent(state: RequestUiState.Declined, onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroCircle(
            iconRes = com.example.designsystem.R.drawable.ic_cancel,
            containerColor = Theme.colors.error.copy(alpha = 0.12f),
            iconTint = Theme.colors.error,
        )
        VerticalSpace(Theme.spacing.medium)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_declined),
            style = Theme.typography.title.copy(color = Theme.colors.primaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.small)
        BasicText(
            text = state.reason ?: stringResource(R.string.meetingrequest_request_declined_subtitle),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.large)
        SecondaryButton(
            caption = stringResource(R.string.meetingrequest_request_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExpiredContent(onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroCircle(
            iconRes = com.example.designsystem.R.drawable.clock_icon,
            containerColor = Theme.colors.amber.copy(alpha = 0.14f),
            iconTint = Theme.colors.amber,
        )
        VerticalSpace(Theme.spacing.medium)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_expired),
            style = Theme.typography.title.copy(color = Theme.colors.primaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.small)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_expired_subtitle),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.large)
        SecondaryButton(
            caption = stringResource(R.string.meetingrequest_request_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EndedContent(onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroCircle(
            iconRes = com.example.designsystem.R.drawable.ic_check,
            containerColor = Theme.colors.primaryContainer,
            iconTint = Theme.colors.primary,
        )
        VerticalSpace(Theme.spacing.medium)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_ended),
            style = Theme.typography.title.copy(color = Theme.colors.primaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.small)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_ended_subtitle),
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.large)
        SecondaryButton(
            caption = stringResource(R.string.meetingrequest_request_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AlreadyPendingContent(
    state: RequestUiState.AlreadyPending,
    onCancelExisting: () -> Unit,
    onBack: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HeroCircle(
            iconRes = com.example.designsystem.R.drawable.clock_icon,
            containerColor = Theme.colors.amber.copy(alpha = 0.14f),
            iconTint = Theme.colors.amber,
        )
        VerticalSpace(Theme.spacing.medium)
        BasicText(
            text = stringResource(R.string.meetingrequest_request_already_pending_title),
            style = Theme.typography.title.copy(color = Theme.colors.primaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.small)
        BasicText(
            text = state.message.ifBlank { stringResource(R.string.meetingrequest_request_already_pending_subtitle) },
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont, textAlign = TextAlign.Center),
        )
        VerticalSpace(Theme.spacing.large)
        PrimaryButton(
            caption = stringResource(R.string.meetingrequest_request_already_pending_cancel),
            onClick = onCancelExisting,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpace(Theme.spacing.small)
        SecondaryButton(
            caption = stringResource(R.string.meetingrequest_request_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HeroCircle(
    iconRes: Int,
    containerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(HeroCircleSize)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconTint),
            modifier = Modifier.size(Theme.size.iconLarge),
        )
    }
}

@Composable
private fun PulsingCountdownRing(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pendingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pendingPulseScale",
    )
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 900),
        label = "pendingCountdownFraction",
    )

    Box(
        modifier = modifier.size(HeroCircleSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(HeroCircleSize)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.10f)),
        )
        Canvas(
            modifier = Modifier
                .size(HeroCircleSize)
                .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale),
        ) {
            val stroke = 4.dp.toPx()
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

@Composable
private fun VerticalSpace(space: Dp) {
    Spacer(modifier = Modifier.height(space))
}
