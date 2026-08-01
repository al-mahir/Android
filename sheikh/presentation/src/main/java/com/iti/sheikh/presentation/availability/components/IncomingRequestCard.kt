package com.iti.sheikh.presentation.availability

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.avatar.InitialsAvatar
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.theme.Theme
import com.iti.sheikh.presentation.R
import kotlinx.coroutines.delay

@Composable
fun IncomingRequestCard(
    studentName: String,
    note: String,
    expiresAt: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalSeconds = remember(expiresAt) {
        val str = if (expiresAt.endsWith("Z") || expiresAt.contains("+")) expiresAt else "${expiresAt}Z"
        val parsed = runCatching { java.time.Instant.parse(str) }.getOrNull() ?: java.time.Instant.now()
        (parsed.epochSecond - java.time.Instant.now().epochSecond).coerceAtLeast(1)
    }
    var remainingSeconds by remember(expiresAt) {
        val str = if (expiresAt.endsWith("Z") || expiresAt.contains("+")) expiresAt else "${expiresAt}Z"
        val parsed = runCatching { java.time.Instant.parse(str) }.getOrNull() ?: java.time.Instant.now()
        val remaining = parsed.epochSecond - java.time.Instant.now().epochSecond
        mutableLongStateOf(remaining.coerceAtLeast(0))
    }

    LaunchedEffect(expiresAt) {
        val str = if (expiresAt.endsWith("Z") || expiresAt.contains("+")) expiresAt else "${expiresAt}Z"
        val parsed = runCatching { java.time.Instant.parse(str) }.getOrNull() ?: java.time.Instant.now()
        while (remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds = (parsed.epochSecond - java.time.Instant.now().epochSecond).coerceAtLeast(0)
        }
    }

    val fraction = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val urgencyColor by animateColorAsState(
        targetValue = when {
            fraction > 0.5f -> Theme.colors.success
            fraction > 0.2f -> Theme.colors.amber
            else -> Theme.colors.error
        },
        animationSpec = tween(durationMillis = 400),
        label = "requestUrgencyColor",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Theme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Theme.colors.backGround),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(
                    initials = studentInitials(studentName),
                    contentDescription = studentName,
                    modifier = Modifier.size(Theme.size.avatarMedium),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Theme.spacing.small),
                ) {
                    BasicText(
                        text = stringResource(R.string.meetingrequest_availability_incoming_request_title, studentName),
                        style = Theme.typography.body.large.copy(color = Theme.colors.primaryFont),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (note.isNotBlank()) {
                        BasicText(
                            text = note,
                            style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                CountdownRing(fraction = fraction, color = urgencyColor) {
                    BasicText(
                        text = remainingSeconds.toString(),
                        style = Theme.typography.body.small.copy(color = urgencyColor),
                    )
                }
            }
            BasicText(
                text = stringResource(R.string.meetingrequest_availability_expires_in_seconds, remainingSeconds),
                style = Theme.typography.body.small.copy(color = urgencyColor),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                SecondaryButton(
                    caption = stringResource(R.string.meetingrequest_availability_incoming_request_decline),
                    onClick = onDecline,
                    iconPainter = painterResource(com.example.designsystem.R.drawable.ic_cancel),
                    height = ButtonHeightCompact,
                    shape = Theme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    caption = stringResource(R.string.meetingrequest_availability_incoming_request_accept),
                    onClick = onAccept,
                    iconPainter = painterResource(com.example.designsystem.R.drawable.ic_check),
                    containerColor = Theme.colors.success,
                    contentColor = Theme.colors.onSuccess,
                    height = ButtonHeightCompact,
                    shape = Theme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun studentInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

@Composable
private fun CountdownRing(
    fraction: Float,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 900),
        label = "countdownFraction",
    )
    Box(
        modifier = modifier
            .size(Theme.size.avatarSmall)
            .clip(RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(Theme.size.avatarSmall)) {
            val stroke = 3.dp.toPx()
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
