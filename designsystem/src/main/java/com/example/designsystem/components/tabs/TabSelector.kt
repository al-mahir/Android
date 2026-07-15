package com.example.designsystem.components.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

private val TabShape: Shape = RoundedCornerShape(10.dp)

/**
 * A horizontal row of pill-style tabs. The selected tab gets a solid `primary` background
 * with `onPrimary` text; unselected tabs render as `primary`-coloured text only.
 *
 * Generic over the label list — pass the strings you want to show and the currently
 * selected index. The `Row` mirrors automatically under RTL.
 */
@Composable
fun TabSelector(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        tabs.forEachIndexed { index, label ->
            TabItem(
                label = label,
                selected = index == selectedIndex,
                onClick = { onTabSelected(index) },
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun RowScope.TabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundModifier = if (selected) {
        Modifier
            .clip(TabShape)
            .background(Theme.colors.primary)
    } else {
        Modifier.clip(TabShape)
            .background(Theme.colors.surface)

    }
    Text(
        text = label,
        textAlign = TextAlign.Center,
        style = Theme.typography.body.medium.copy(
            color = if (selected) Theme.colors.onPrimary else Theme.colors.primary,
            fontWeight = FontWeight.Medium,
        ),
        modifier = backgroundModifier
            .clickable(onClick = onClick)
            .weight(1f)
            .padding(vertical = 8.dp),
    )
}

// ─── Previews ─────────────────────────────────────────────────────────────────

private val PreviewTabs = listOf("استكمال حجزك", "الحالية", "المنتهية")

@Preview(name = "TabSelector – RTL", showBackground = true)
@Composable
private fun TabSelectorRtlPreview() {
    AlMahirTheme(locale = Locale("ar")) {
        var selected by remember { mutableIntStateOf(1) }
        TabSelector(
            tabs = PreviewTabs,
            selectedIndex = selected,
            onTabSelected = { selected = it },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "TabSelector – LTR", showBackground = true)
@Composable
private fun TabSelectorLtrPreview() {
    AlMahirTheme {
        var selected by remember { mutableIntStateOf(0) }
        TabSelector(
            tabs = listOf("Incomplete", "Current", "Expired"),
            selectedIndex = selected,
            onTabSelected = { selected = it },
            modifier = Modifier.padding(16.dp),
        )
    }
}
