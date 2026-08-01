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
import com.iti.meeting.domain.model.ActiveCallRecord
import com.iti.presentation.R

private val CardShape = RoundedCornerShape(20.dp)

/** Home-screen prompt for case 3 in docs/Meeting-Call-Lifecycle-Plan.md's reopen decision tree:
 * the app process died mid-call, so all we have left is the persisted [ActiveCallRecord] — never
 * auto-rejoins, since the call may well have already ended server-side while the process was
 * dead. Structurally mirrors [PendingMeetingRequestCard]. */
@Composable
fun OngoingCallCard(
    call: ActiveCallRecord,
    onRejoin: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    painter = painterResource(com.example.designsystem.R.drawable.ic_mic),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Theme.colors.amber),
                    modifier = Modifier.size(Theme.size.iconMedium),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = stringResource(R.string.home_active_call_title),
                    style = Theme.typography.body.large.copy(color = Theme.colors.primaryFont),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BasicText(
                    text = call.remoteDisplayName?.let {
                        stringResource(R.string.home_active_call_subtitle, it)
                    } ?: stringResource(R.string.home_active_call_subtitle_generic),
                    style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            SecondaryButton(
                caption = stringResource(R.string.home_active_call_dismiss),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                caption = stringResource(R.string.home_active_call_rejoin),
                onClick = onRejoin,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
