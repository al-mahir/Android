package com.example.mushaf.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.presentation.R

@Composable
fun MushafBottomBar(
    visible: Boolean,
    mushafMode: MushafMode,
    areAyahsVisible: Boolean,
    isRecordingActive: Boolean,
    onToggleAyahVisibility: () -> Unit,
    onRevealNextWord: () -> Unit,
    onRevealNextAyah: () -> Unit,
    onModeSelected: (MushafMode) -> Unit,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.colors.surface.copy(alpha = 0.96f))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            EyeSection(
                areAyahsVisible = areAyahsVisible,
                onToggleAyahVisibility = onToggleAyahVisibility,
                onRevealNextWord = onRevealNextWord,
                onRevealNextAyah = onRevealNextAyah,
            )

            MushafModeSelector(
                selectedMode = mushafMode,
                onModeSelected = onModeSelected,
            )

            MicSection(
                mushafMode = mushafMode,
                isRecordingActive = isRecordingActive,
                onToggleRecording = onToggleRecording,
            )
        }
    }
}

@Composable
private fun EyeSection(
    areAyahsVisible: Boolean,
    onToggleAyahVisibility: () -> Unit,
    onRevealNextWord: () -> Unit,
    onRevealNextAyah: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleAyahVisibility) {
            Icon(
                imageVector = if (areAyahsVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                contentDescription = stringResource(
                    if (areAyahsVisible) R.string.mushaf_cd_hide_ayahs else R.string.mushaf_cd_show_ayahs,
                ),
                tint = if (areAyahsVisible) Theme.colors.onSurface else Theme.colors.primary,
                modifier = Modifier.size(Theme.size.iconMedium),
            )
        }

        AnimatedVisibility(
            visible = !areAyahsVisible,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
        ) {
            Row {
                IconButton(onClick = onRevealNextWord) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.mushaf_cd_next_word),
                        tint = Theme.colors.primary,
                        modifier = Modifier.size(Theme.size.iconMedium),
                    )
                }
                IconButton(onClick = onRevealNextAyah) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardDoubleArrowLeft,
                        contentDescription = stringResource(R.string.mushaf_cd_next_ayah),
                        tint = Theme.colors.primary,
                        modifier = Modifier.size(Theme.size.iconMedium),
                    )
                }
            }
        }
    }
}

@Composable
private fun MicSection(
    mushafMode: MushafMode,
    isRecordingActive: Boolean,
    onToggleRecording: () -> Unit,
) {
    val showMic = mushafMode == MushafMode.RECITATION || mushafMode == MushafMode.MUALLEM
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic_scale",
    )

    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = showMic,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it },
        ) {
            FloatingActionButton(
                onClick = onToggleRecording,
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .then(if (isRecordingActive) Modifier.scale(scale) else Modifier),
                containerColor = if (isRecordingActive) Theme.colors.error else Theme.colors.primary,
                contentColor = Theme.colors.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp,
                ),
            ) {
                Icon(
                    imageVector = if (isRecordingActive) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = stringResource(
                        if (isRecordingActive) R.string.mushaf_cd_stop_recording else R.string.mushaf_cd_start_recording,
                    ),
                    modifier = Modifier.size(Theme.size.iconMedium),
                )
            }
        }
    }
}
