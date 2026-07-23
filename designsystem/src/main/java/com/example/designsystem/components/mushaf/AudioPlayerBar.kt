package com.example.designsystem.components.mushaf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme

/**
 * A floating playback control bar used in Listen Mode.
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(Theme.spacing.medium),
        shape = RoundedCornerShape(16.dp),
        color = Theme.colors.surface,
        contentColor = Theme.colors.secondaryFont,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(Theme.spacing.medium)
        ) {
            // Top Row: Reciter & Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reciter Chip
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onReciterClick)
                        .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Theme.colors.primaryContainer)
                    )
                    Text(
                        text = reciterName.ifEmpty { "Select Reciter" },
                        style = Theme.typography.body.large,
                        color = Theme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                // Speed Chip
                Surface(
                    modifier = Modifier.padding(start = Theme.spacing.small).clickable(onClick = onSpeedClick),
                    shape = RoundedCornerShape(8.dp),
                    color = Theme.colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Theme.colors.outline)
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        style = Theme.typography.body.medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Theme.spacing.small))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevClick) {
                    Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Previous")
                }
                
                Spacer(modifier = Modifier.width(Theme.spacing.medium))
                
                FilledIconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Theme.colors.primary,
                        contentColor = Theme.colors.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(Theme.spacing.medium))
                
                IconButton(onClick = onNextClick) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}
