package com.example.mushaf.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.R

@Composable
fun GradingModeToggle(
    tajweedGradingEnabled: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,

    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(ToggleShape)
            .background(Theme.colors.field)
            .border(1.dp, Theme.colors.border, ToggleShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GradingModeOption(
            label = stringResource(R.string.recite_grading_hifz_only),
            selected = !tajweedGradingEnabled,
            enabled = enabled,
            onClick = { onSelect(false) },
        )
        GradingModeOption(
            label = stringResource(R.string.recite_grading_with_tajweed),
            selected = tajweedGradingEnabled,
            enabled = enabled,
            onClick = { onSelect(true) },
        )
    }
}

@Composable
private fun GradingModeOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) Theme.colors.primary else Color.Transparent
    val content = when {
        !enabled -> Theme.colors.hint
        selected -> Theme.colors.onPrimary
        else -> Theme.colors.secondaryFont
    }

    Text(
        text = label,
        style = Theme.typography.body.small,
        color = content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(ToggleShape)
            .background(background)
            .selectable(
                selected = selected,
                enabled = enabled && !selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

private val ToggleShape = RoundedCornerShape(50)
