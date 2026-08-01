package com.iti.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.theme.Theme
import com.iti.meeting.domain.model.PendingMeetingRequest
import com.iti.presentation.R
import kotlinx.coroutines.delay

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun PendingMeetingRequestCard(
    request: PendingMeetingRequest,
    onView: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var remainingSeconds by remember(request.expiresAt) {
        val parsed = runCatching { java.time.Instant.parse(request.expiresAt) }.getOrNull()
        mutableLongStateOf(parsed?.let { (it.epochSecond - java.time.Instant.now().epochSecond).coerceAtLeast(0) } ?: 0L)
    }
    LaunchedEffect(request.expiresAt) {
        val parsed = runCatching { java.time.Instant.parse(request.expiresAt) }.getOrNull() ?: return@LaunchedEffect
        while (remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds = (parsed.epochSecond - java.time.Instant.now().epochSecond).coerceAtLeast(0)
        }
    }
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Theme.colors.amber.copy(alpha = 0.10f))
            .padding(Theme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            Row(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Theme.colors.amber.copy(alpha = 0.18f)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(com.example.designsystem.R.drawable.clock_icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Theme.colors.amber),
                    modifier = Modifier.size(Theme.size.iconMedium),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = stringResource(R.string.home_pending_meeting_title),
                    style = Theme.typography.body.large.copy(color = Theme.colors.primaryFont),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = request.sheikhName?.let {
                        stringResource(R.string.home_pending_meeting_subtitle, it)
                    } ?: stringResource(R.string.home_pending_meeting_subtitle_generic),
                    style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicText(
                text = stringResource(
                    R.string.home_pending_meeting_countdown,
                    minutes,
                    seconds,
                ),
                style = Theme.typography.body.small.copy(color = Theme.colors.amber),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            SecondaryButton(
                caption = stringResource(R.string.home_pending_meeting_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                caption = stringResource(R.string.home_pending_meeting_view),
                onClick = onView,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
