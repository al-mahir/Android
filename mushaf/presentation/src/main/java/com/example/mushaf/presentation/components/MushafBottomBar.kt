package com.example.mushaf.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import com.example.designsystem.R as DesignsystemR
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

private const val MIC_LEVEL_SCALE_RANGE = 0.35f

private const val MIC_LEVEL_VISIBLE_THRESHOLD = 0.05f

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
    micLevel: Float = 0f,
    onModeTabPositioned: ((MushafMode, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
    





 
    statusRow: (@Composable () -> Unit)? = null,

    canFinishSession: Boolean = false,
    onFinishSession: () -> Unit = {},

    gradingToggle: (@Composable () -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Theme.colors.surface.copy(alpha = 0.96f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            statusRow?.let { row ->
                Box(modifier = Modifier.padding(bottom = 8.dp)) { row() }
            }

            gradingToggle?.let { toggle ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { toggle() }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    onTabPositioned = onModeTabPositioned,
                )

                MicSection(
                    mushafMode = mushafMode,
                    isRecordingActive = isRecordingActive,
                    micLevel = micLevel,
                    canFinishSession = canFinishSession,
                    onFinishSession = onFinishSession,
                    onToggleRecording = onToggleRecording,
                )
            }
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(
            onClick = onToggleAyahVisibility,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(if (areAyahsVisible) DesignsystemR.drawable.ic_eye else DesignsystemR.drawable.ic_eye_closed),
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
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onRevealNextWord,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(DesignsystemR.drawable.ic_step_word),
                        contentDescription = stringResource(R.string.mushaf_cd_next_word),
                        tint = Theme.colors.primary,
                        modifier = Modifier.size(Theme.size.iconMedium),
                    )
                }
                IconButton(
                    onClick = onRevealNextAyah,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(DesignsystemR.drawable.ic_step_ayah),
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
    micLevel: Float,
    canFinishSession: Boolean,
    onFinishSession: () -> Unit,
    onToggleRecording: () -> Unit,
) {
    val showMic = mushafMode == MushafMode.RECITATION || mushafMode == MushafMode.MUALLEM

    
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mic_scale",
    )

    val levelScale by animateFloatAsState(
        targetValue = 1f + micLevel.coerceIn(0f, 1f) * MIC_LEVEL_SCALE_RANGE,
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "mic_level_scale",
    )

    val scale = if (isRecordingActive && micLevel > MIC_LEVEL_VISIBLE_THRESHOLD) {
        levelScale
    } else {
        idleScale
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(
            visible = canFinishSession,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it },
        ) {
            IconButton(onClick = onFinishSession) {
                Icon(
                    painter = painterResource(DesignsystemR.drawable.ic_flag),
                    contentDescription = stringResource(R.string.mushaf_cd_finish_session),
                    tint = Theme.colors.primary,
                    modifier = Modifier.size(Theme.size.iconMedium),
                )
            }
        }

        MicButton(
            showMic = showMic,
            isRecordingActive = isRecordingActive,
            scale = scale,
            onToggleRecording = onToggleRecording,
        )
    }
}




 
@Composable
private fun MicButton(
    showMic: Boolean,
    isRecordingActive: Boolean,
    scale: Float,
    onToggleRecording: () -> Unit,
) {
    
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
                    painter = painterResource(if (isRecordingActive) DesignsystemR.drawable.ic_mic_off else DesignsystemR.drawable.ic_mic),
                    contentDescription = stringResource(
                        if (isRecordingActive) R.string.mushaf_cd_stop_recording else R.string.mushaf_cd_start_recording,
                    ),
                    modifier = Modifier.size(Theme.size.iconMedium),
                )
            }
        }
    }
}
