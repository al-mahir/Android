package com.example.mushaf.presentation.muallem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.R as DesignsystemR
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.R

@Composable
fun MuallemSessionBar(
    session: MuallemSessionState,
    isConnecting: Boolean,
    isActive: Boolean,
    onStopSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.small)
            .background(Theme.colors.surface.copy(alpha = 0.95f))
            .border(1.dp, Theme.colors.border, Theme.shapes.small)
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.extraSmall),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        // ── Top row: ayah info + status + stop button ────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)
            ) {
                BasicText(
                    text = stringResource(
                        R.string.muallem_session_ayah_info,
                        session.surah,
                        session.currentAyah,
                    ),
                    style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                )

                // Socket Status Indicator
                val dotColor = when {
                    isConnecting -> Theme.colors.warning
                    isActive -> Theme.colors.success
                    else -> Theme.colors.error
                }
                val statusText = when {
                    isConnecting -> "Connecting..."
                    isActive -> "Connected"
                    else -> "Disconnected"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
                    BasicText(
                        text = statusText,
                        style = Theme.typography.body.small.copy(color = dotColor)
                    )
                }
            }

            IconButton(
                onClick = onStopSession,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    painter = painterResource(DesignsystemR.drawable.ic_cancel),
                    contentDescription = stringResource(R.string.muallem_stop_session),
                    tint = Theme.colors.error,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // ── Phase label ───────────────────────────────────────────────────────
        AnimatedContent(
            targetState = session.phase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "muallem_phase",
        ) { phase ->
            when (phase) {
                MuallemPhase.SheikhPlaying -> PhaseLabel(
                    text = stringResource(R.string.muallem_phase_sheikh_playing),
                    color = Theme.colors.primary,
                )
                is MuallemPhase.UserRecording -> PhaseLabel(
                    text = stringResource(
                        R.string.muallem_phase_user_recording,
                        phase.repeatIndex,
                        session.repeatCount,
                    ),
                    color = Theme.colors.error,
                )
                is MuallemPhase.ShowingFeedback -> {
                    val last = session.lastFeedback
                    val text = if (last != null) {
                        val pct = last.accuracy.toInt()
                        stringResource(R.string.muallem_phase_feedback, pct)
                    } else {
                        stringResource(R.string.muallem_phase_feedback_done)
                    }
                    PhaseLabel(
                        text = text,
                        color = when {
                            last == null || last.accuracy >= 90f -> Theme.colors.success
                            last.accuracy >= 70f -> Theme.colors.warning
                            else -> Theme.colors.error
                        },
                    )
                }
            }
        }

        // ── Repeat dots ───────────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(session.repeatCount) { index ->
                val dotIndex = index + 1
                val isCompleted = dotIndex <= session.repeatFeedbacks.size
                val isActiveDot = dotIndex == session.currentRepeat &&
                        session.phase is MuallemPhase.UserRecording
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> Theme.colors.primary
                                isActiveDot -> Theme.colors.error
                                else        -> Theme.colors.border
                            }
                        ),
                )
            }
        }
    }
}

@Composable
private fun PhaseLabel(text: String, color: androidx.compose.ui.graphics.Color) {
    BasicText(
        text = text,
        style = Theme.typography.body.medium.copy(
            color = color,
            textAlign = TextAlign.Start,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}