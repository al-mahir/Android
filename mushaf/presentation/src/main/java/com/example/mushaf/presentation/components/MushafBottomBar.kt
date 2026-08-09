package com.example.mushaf.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.example.designsystem.R as DesignsystemR
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.presentation.R

private const val MIC_LEVEL_SCALE_RANGE = 0.35f

private const val MIC_LEVEL_VISIBLE_THRESHOLD = 0.05f

/**
 * Width of the fixed leading/trailing control slots in the main bar row. Both slots are laid out
 * before the (weighted) mode selector, so neither the eye toggle nor the mic can ever be squeezed
 * by a wider neighbour.
 */
private val CONTROL_SLOT_WIDTH = 48.dp

/** Trailing slot width when the "finish session" action shares the slot with the mic. */
private val CONTROL_SLOT_WIDTH_WITH_FINISH = 92.dp

@Composable
fun MushafBottomBar(
    visible: Boolean,
    mushafMode: MushafMode,
    areAyahsVisible: Boolean,
    isRecordingActive: Boolean,
    /** False while the Mu'allem session, not the user, is in charge of the mic. */
    isMicEnabled: Boolean,
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

    /** Mu'allem session status bar, rendered above statusRow/gradingToggle. */
    muallemBar: (@Composable () -> Unit)? = null,
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
            muallemBar?.let { bar ->
                Box(modifier = Modifier.padding(bottom = 8.dp)) { bar() }
            }

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

            // The reveal steppers live on their own row so they never compete for width with the
            // mode selector or the mic button in the row below.
            AnimatedVisibility(
                visible = !areAyahsVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                RevealActionsRow(
                    onRevealNextWord = onRevealNextWord,
                    onRevealNextAyah = onRevealNextAyah,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier.width(CONTROL_SLOT_WIDTH),
                    contentAlignment = Alignment.Center,
                ) {
                    EyeToggle(
                        areAyahsVisible = areAyahsVisible,
                        onToggleAyahVisibility = onToggleAyahVisibility,
                    )
                }

                MushafModeSelector(
                    selectedMode = mushafMode,
                    onModeSelected = onModeSelected,
                    onTabPositioned = onModeTabPositioned,
                    modifier = Modifier.weight(1f),
                )

                MicSection(
                    mushafMode = mushafMode,
                    isRecordingActive = isRecordingActive,
                    isMicEnabled = isMicEnabled,
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
private fun EyeToggle(
    areAyahsVisible: Boolean,
    onToggleAyahVisibility: () -> Unit,
) {
    IconButton(
        onClick = onToggleAyahVisibility,
        modifier = Modifier.size(CONTROL_SLOT_WIDTH),
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
}

/**
 * Full-width row of labelled reveal actions, shown while the memorisation veil is on. Labels make
 * the two steppers self-explanatory, which icon-only buttons crammed next to the eye were not.
 */
@Composable
private fun RevealActionsRow(
    onRevealNextWord: () -> Unit,
    onRevealNextAyah: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RevealChip(
            iconRes = DesignsystemR.drawable.ic_step_word,
            label = stringResource(R.string.mushaf_reveal_next_word),
            contentDescription = stringResource(R.string.mushaf_cd_next_word),
            onClick = onRevealNextWord,
            modifier = Modifier.weight(1f),
        )
        RevealChip(
            iconRes = DesignsystemR.drawable.ic_step_ayah,
            label = stringResource(R.string.mushaf_reveal_next_ayah),
            contentDescription = stringResource(R.string.mushaf_cd_next_ayah),
            onClick = onRevealNextAyah,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RevealChip(
    iconRes: Int,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Theme.colors.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Theme.colors.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = Theme.typography.body.small,
            color = Theme.colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun MicSection(
    mushafMode: MushafMode,
    isRecordingActive: Boolean,
    isMicEnabled: Boolean,
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

    // Reserving the slot width up front (instead of letting the row shrink us) keeps the mic at a
    // full-size touch target whatever else is on screen.
    val slotWidth by animateDpAsState(
        targetValue = if (canFinishSession) CONTROL_SLOT_WIDTH_WITH_FINISH else CONTROL_SLOT_WIDTH,
        label = "mic_slot_width",
    )

    Row(
        modifier = Modifier.width(slotWidth),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        AnimatedVisibility(
            visible = canFinishSession,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it },
        ) {
            IconButton(onClick = onFinishSession, modifier = Modifier.size(40.dp)) {
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
            enabled = isMicEnabled,
            scale = scale,
            onToggleRecording = onToggleRecording,
        )
    }
}





@Composable
private fun MicButton(
    showMic: Boolean,
    isRecordingActive: Boolean,
    enabled: Boolean,
    scale: Float,
    onToggleRecording: () -> Unit,
) {

    Box(modifier = Modifier.size(CONTROL_SLOT_WIDTH), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = showMic,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it },
        ) {
            val containerColor by animateColorAsState(
                targetValue = when {
                    !enabled -> Theme.colors.surfaceVariant
                    isRecordingActive -> Theme.colors.error
                    else -> Theme.colors.primary
                },
                label = "mic_container_color",
            )
            val iconColor by animateColorAsState(
                targetValue = if (enabled) Theme.colors.onPrimary else Theme.colors.secondaryFont,
                label = "mic_icon_color",
            )

            // Material3's FloatingActionButton takes no `enabled`, and a no-op onClick would still
            // ripple as if it had worked. Surface's clickable overload gives real disabled
            // behaviour: no click, no ripple, and announced as disabled to accessibility services.
            Surface(
                onClick = onToggleRecording,
                enabled = enabled,
                shape = CircleShape,
                color = containerColor,
                contentColor = iconColor,
                shadowElevation = if (enabled) 4.dp else 0.dp,
                modifier = Modifier
                    .size(44.dp)
                    .then(if (isRecordingActive && enabled) Modifier.scale(scale) else Modifier),
            ) {
                Box(contentAlignment = Alignment.Center) {
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
}