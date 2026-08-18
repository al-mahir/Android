package com.example.designsystem.components.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

/**
 * Horizontally-scrolling, single-select filter chip bar. Exactly one option is selected
 * at a time — the chip whose [value] equals [selectedValue]. Selected chips fill with
 * `primary`/`onPrimary`; unselected chips sit on `surface` with `secondaryFont` text.
 *
 * Generic over the option value so features pass their own enums, or `null` for an
 * "All" option. The row scrolls horizontally and mirrors automatically under RTL.
 */
@Composable
fun <T> FilterChips(
    options: List<Pair<T, String>>,
    selectedValue: T,
    onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        items(options) { (value, label) ->
            val selected = value == selectedValue
            Box(
                modifier = Modifier
                    .clip(Theme.shapes.extraLarge)
                    .background(if (selected) Theme.colors.primary else Theme.colors.surface)
                    .clickable { onValueSelected(value) }
                    .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = label,
                    style = Theme.typography.body.small.copy(
                        color = if (selected) Theme.colors.onPrimary else Theme.colors.secondaryFont,
                    ),
                )
            }
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

private enum class PreviewStatus { SCHEDULED, ONGOING, COMPLETED }

private val PreviewOptions: List<Pair<PreviewStatus?, String>> = listOf(
    null to "All",
    PreviewStatus.SCHEDULED to "Scheduled",
    PreviewStatus.ONGOING to "Ongoing",
    PreviewStatus.COMPLETED to "Completed",
)

private val PreviewOptionsAr: List<Pair<PreviewStatus?, String>> = listOf(
    null to "الكل",
    PreviewStatus.SCHEDULED to "مجدولة",
    PreviewStatus.ONGOING to "جارية",
    PreviewStatus.COMPLETED to "منتهية",
)

@Preview(name = "FilterChips – RTL", showBackground = true)
@Composable
private fun FilterChipsRtlPreview() {
    AlMahirTheme(locale = Locale("ar")) {
        var selected by remember { mutableStateOf<PreviewStatus?>(PreviewStatus.SCHEDULED) }
        FilterChips(
            options = PreviewOptionsAr,
            selectedValue = selected,
            onValueSelected = { selected = it },
            modifier = Modifier.padding(Theme.spacing.medium),
        )
    }
}

@Preview(name = "FilterChips – LTR", showBackground = true)
@Composable
private fun FilterChipsLtrPreview() {
    AlMahirTheme {
        var selected by remember { mutableStateOf<PreviewStatus?>(PreviewStatus.ONGOING) }
        FilterChips(
            options = PreviewOptions,
            selectedValue = selected,
            onValueSelected = { selected = it },
            modifier = Modifier.padding(Theme.spacing.medium),
        )
    }
}
