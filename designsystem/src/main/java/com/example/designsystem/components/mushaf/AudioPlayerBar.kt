package com.example.designsystem.components.mushaf

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.example.designsystem.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.designsystem.theme.Theme

/**
 * A compact, elegant floating playback control pill used in Listen Mode.
 *
 * Single-row layout:
 *   [ReciterName]  ‹ prev ›  ◉ play/pause  ‹ next ›  [speed]
 */
@Composable
fun AudioPlayerBar(
    isPlaying: Boolean,
    reciterName: String,
    playbackSpeed: Float,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onReciterClick: () -> Unit,
    onSpeedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillBg = Theme.colors.surface
    val primaryColor = Theme.colors.primary
    val onPrimary = Theme.colors.onPrimary

    // Animate play/pause button background
    val playBtnColor by animateColorAsState(
        targetValue = if (isPlaying) primaryColor else Theme.colors.primaryContainer,
        animationSpec = tween(200),
        label = "playBtnColor",
    )
    val playIconColor by animateColorAsState(
        targetValue = if (isPlaying) onPrimary else primaryColor,
        animationSpec = tween(200),
        label = "playIconColor",
    )

    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(50.dp)),
        shape = RoundedCornerShape(50.dp),
        color = pillBg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {

            // ── Reciter name (tappable, truncated) ────────────────────────
            Text(
                text = reciterName.ifEmpty { androidx.compose.ui.res.stringResource(R.string.audio_player_select_reciter) },
                style = Theme.typography.body.small.copy(fontSize = 11.sp),
                color = primaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onReciterClick,
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )



            // ── Prev ──────────────────────────────────────────────────────
            IconButton(
                onClick = onPrevClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_skip_previous),
                    contentDescription = androidx.compose.ui.res.stringResource(R.string.audio_player_prev),
                    tint = Theme.colors.secondaryFont,
                    modifier = Modifier.size(20.dp),
                )
            }

            // ── Play / Pause ───────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(playBtnColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPlayPauseClick,
                    ),
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (isPlaying) {
                        androidx.compose.ui.res.stringResource(R.string.audio_player_pause)
                    } else {
                        androidx.compose.ui.res.stringResource(R.string.audio_player_play)
                    },
                    tint = playIconColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            // ── Next ───────────────────────────────────────────────────────
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_skip_next),
                    contentDescription = androidx.compose.ui.res.stringResource(R.string.audio_player_next),
                    tint = Theme.colors.secondaryFont,
                    modifier = Modifier.size(20.dp),
                )
            }

            // ── Speed chip ────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Theme.colors.surfaceVariant)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSpeedClick,
                    )
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(
                    text = formatSpeed(playbackSpeed),
                    style = Theme.typography.body.small.copy(fontSize = 11.sp),
                    color = Theme.colors.onSurface,
                )
            }
        }
    }
}

private fun formatSpeed(speed: Float): String {
    return if (speed == speed.toLong().toFloat()) "${speed.toLong()}×" else "${speed}×"
}
