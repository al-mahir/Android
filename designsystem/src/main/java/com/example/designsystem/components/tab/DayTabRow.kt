package com.example.designsystem.components.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.example.designsystem.theme.Theme

/**
 * A row of pill-shaped tabs (day-of-week selector for the meal plan, in the consultation
 * screen). [labels] supplies the tab text, [selectedIndex] marks the active tab, and
 * tapping a tab fires [onSelect]. RTL-safe via the row's default layout direction.
 *
 * By default the tabs size to their content and the row scrolls horizontally when they
 * overflow. Set [fillEqually] to spread a small, fixed set of tabs across the full width
 * with each tab taking an equal share (no scrolling) — used by the meals screen.
 */
@Composable
fun DayTabRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fillEqually: Boolean = false,
) {
    Row(
        modifier = if (fillEqually) {
            modifier.fillMaxWidth().padding(vertical = 4.dp)
        } else {
            modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            DayTab(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = if (fillEqually) Modifier.weight(1f) else Modifier,
            )
        }
    }
}

@Composable
private fun RowScope.DayTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) Theme.colors.primary else Theme.colors.surfaceVariant
    val textColor = if (selected) Theme.colors.onPrimary else Theme.colors.secondaryFont
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else Theme.colors.hint.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = Theme.typography.body.medium.copy(
                color = textColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
    }
}
