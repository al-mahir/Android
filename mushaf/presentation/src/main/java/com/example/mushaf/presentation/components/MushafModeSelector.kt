package com.example.mushaf.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.presentation.R

@Composable
fun MushafModeSelector(
    selectedMode: MushafMode,
    onModeSelected: (MushafMode) -> Unit,
    modifier: Modifier = Modifier,
    onTabPositioned: ((MushafMode, androidx.compose.ui.layout.LayoutCoordinates) -> Unit)? = null,
) {
    val containerShape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .clip(containerShape)
            .background(Theme.colors.surfaceVariant)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MushafMode.entries.forEach { mode ->
            ModeTab(
                label = mode.label(),
                selected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
                modifier = Modifier.onGloballyPositioned { coords ->
                    onTabPositioned?.invoke(mode, coords)
                }
            )
        }
    }
}

@Composable
private fun ModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) Theme.colors.primary else Theme.colors.surfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "mode_tab_bg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Theme.colors.onPrimary else Theme.colors.secondaryFont,
        animationSpec = tween(durationMillis = 200),
        label = "mode_tab_text",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = Theme.typography.body.small,
            color = textColor,
        )
    }
}

@Composable
private fun MushafMode.label(): String = when (this) {
    MushafMode.READING -> stringResource(R.string.mushaf_mode_reading)
    MushafMode.LISTEN -> stringResource(R.string.mushaf_mode_listen)
    MushafMode.RECITATION -> stringResource(R.string.mushaf_mode_recitation)
    MushafMode.MUALLEM -> stringResource(R.string.mushaf_mode_muallem)
}
