package com.example.designsystem.components.placeholderscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme

/**
 * Internal preview-only showcase. Each placeholder screen is rendered at a
 * fixed height inside a bordered preview card so all three variants can be
 * inspected on a single scrolling screen. Toggle chips flip the optional
 * action button on each section.
 */
@Composable
internal fun PlaceholderScreensShowcaseScreen(modifier: Modifier = Modifier) {

    var networkActionVisible by remember { mutableStateOf(true) }
    var searchActionVisible by remember { mutableStateOf(false) }
    var dataActionVisible by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {

        // 1. Network Error
        ShowcaseSectionHeader("Network Error")
        StateChipRow {
            StateChip(
                label = "Retry button",
                active = networkActionVisible,
                onClick = { networkActionVisible = !networkActionVisible },
            )
        }
        PreviewCard {
            NetworkErrorScreen(
                onRetry = if (networkActionVisible) ({ /* preview no-op */ }) else null,
            )
        }

        ShowcaseDivider()

        // 2. Empty Search
        ShowcaseSectionHeader("Empty Search")
        StateChipRow {
            StateChip(
                label = "Clear search button",
                active = searchActionVisible,
                onClick = { searchActionVisible = !searchActionVisible },
            )
        }
        PreviewCard {
            EmptySearchScreen(
                actionButtonText = if (searchActionVisible) "Clear search" else null,
                onActionClick = if (searchActionVisible) ({ /* preview no-op */ }) else null,
            )
        }

        ShowcaseDivider()

        // 3. Empty Data
        ShowcaseSectionHeader("Empty Data")
        StateChipRow {
            StateChip(
                label = "Create button",
                active = dataActionVisible,
                onClick = { dataActionVisible = !dataActionVisible },
            )
        }
        PreviewCard {
            EmptyDataScreen(
                actionButtonText = if (dataActionVisible) "Create new" else null,
                onActionClick = if (dataActionVisible) ({ /* preview no-op */ }) else null,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Bounds the placeholder screen to a fixed preview height so it can sit
 * inside a verticalScroll without crashing on `fillMaxSize` inside the
 * `EmptyState` it wraps.
 */
@Composable
private fun PreviewCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .clip(Theme.shapes.large)
            .border(1.dp, Theme.colors.hint.copy(alpha = 0.3f), Theme.shapes.large)
            .background(Theme.colors.backGround),
    ) {
        content()
    }
}

@Composable
private fun StateChipRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun StateChip(label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) Theme.colors.primary else Theme.colors.backGround
    val border = if (active) Theme.colors.primary else Theme.colors.hint
    val text = if (active) Theme.colors.onPrimary else Theme.colors.hint
    Box(
        modifier = Modifier
            .clip(Theme.shapes.small)
            .background(bg)
            .border(1.dp, border, Theme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = label, style = Theme.typography.body.small.copy(color = text))
    }
}

@Composable
private fun ShowcaseSectionHeader(title: String) {
    BasicText(
        text = title,
        style = Theme.typography.body.large.copy(color = Theme.colors.primary),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ShowcaseDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Theme.colors.hint.copy(alpha = 0.3f)),
    )
}
