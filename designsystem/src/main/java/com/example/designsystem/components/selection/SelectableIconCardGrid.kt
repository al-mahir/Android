package com.example.designsystem.components.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme


data class SelectableIconCardItem(
    val id: String,
    val label: String,
    val iconPainter: Painter,
    val iconBackgroundColor: Color,
    val iconTint: Color? = null,
)

/**
 * A fixed-column grid of [SelectableIconCard]s for a single-select choice (payment method,
 * card brand, …). Rows wrap automatically under RTL since it's built on [Row]/[Arrangement].
 */
@Composable
fun SelectableIconCardGrid(
    items: List<SelectableIconCardItem>,
    selectedId: String?,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
        items.chunked(columns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                rowItems.forEach { item ->
                    SelectableIconCard(
                        label = item.label,
                        iconPainter = item.iconPainter,
                        selected = item.id == selectedId,
                        onClick = { onItemSelected(item.id) },
                        iconBackgroundColor = item.iconBackgroundColor,
                        iconTint = item.iconTint,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Pad an incomplete last row so items keep a consistent width instead of stretching.
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f).width(0.dp))
                }
            }
        }
    }
}
