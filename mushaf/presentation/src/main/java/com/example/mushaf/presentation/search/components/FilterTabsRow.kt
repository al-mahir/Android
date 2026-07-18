package com.example.mushaf.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.MushafFilter

@Composable
fun FilterTabsRow(
    selected: MushafFilter,
    onSelect: (MushafFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MushafFilter.entries.forEach { filter ->
            FilterChip(
                filter = filter,
                isSelected = filter == selected,
                onClick = { onSelect(filter) }
            )
        }
    }
}

@Composable
private fun FilterChip(
    filter: MushafFilter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Theme.colors.primary else Theme.colors.surface
    val textColor = if (isSelected) Theme.colors.onPrimary else Theme.colors.primary
    val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = filter.label,
            color = textColor,
            style = Theme.typography.body.medium.copy(fontWeight = fontWeight)
        )
    }
}
