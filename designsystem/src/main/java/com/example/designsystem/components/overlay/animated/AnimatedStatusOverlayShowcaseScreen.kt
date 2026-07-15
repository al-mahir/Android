package com.example.designsystem.components.overlay.animated

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.theme.Theme
import kotlinx.coroutines.delay

/**
 * Internal preview-only showcase. Tap a button to trigger each overlay
 * scenario: loading-only (manual dismiss), loading then success, or
 * loading then error. The transition variants flip to the terminal
 * state after a short delay to mimic an in-flight request.
 */
@Composable
internal fun AnimatedStatusOverlayShowcaseScreen(modifier: Modifier = Modifier) {

    var overlayState by remember { mutableStateOf<OverlayState?>(null) }
    var pendingTerminal by remember { mutableStateOf<OverlayState?>(null) }

    // After the loading phase, swap to the queued terminal state.
    LaunchedEffect(pendingTerminal) {
        val next = pendingTerminal ?: return@LaunchedEffect
        delay(2_000)
        overlayState = next
        pendingTerminal = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        ShowcaseSectionHeader("Animated Status Overlay")
        ShowcaseHint(
            "Loading is non-dismissible. Success/Error auto-dismiss when the " +
                "Lottie animation finishes; tapping outside or pressing back also dismisses them."
        )

        ShowcaseDivider()

        // 1. Loading only — stays until the user dismisses (back press is blocked,
        // tap-outside is blocked; here we expose a manual "Hide" button).
        ShowcaseSectionHeader("1. Loading only")
        PrimaryButton(
            caption = "Show Loading",
            onClick = {
                pendingTerminal = null
                overlayState = OverlayState.Loading
            },
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            caption = "Hide Loading",
            onClick = { overlayState = null },
            modifier = Modifier.fillMaxWidth(),
            isDisabled = overlayState !is OverlayState.Loading || pendingTerminal != null,
        )

        ShowcaseDivider()

        // 2. Loading → Success after a delay (with message).
        ShowcaseSectionHeader("2. Loading → Success (2s)")
        PrimaryButton(
            caption = "Show Loading → Success",
            onClick = {
                overlayState = OverlayState.Loading
                pendingTerminal = OverlayState.Success("Saved successfully")
            },
            modifier = Modifier.fillMaxWidth(),
        )

        ShowcaseDivider()

        // 3. Loading → Error after a delay (with message).
        ShowcaseSectionHeader("3. Loading → Error (2s)")
        PrimaryButton(
            caption = "Show Loading → Error",
            onClick = {
                overlayState = OverlayState.Loading
                pendingTerminal = OverlayState.Error("Something went wrong")
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
    }

    StatusOverlay(
        state = overlayState,
        onDismiss = {
            overlayState = null
            pendingTerminal = null
        },
    )
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun ShowcaseSectionHeader(title: String) {
    BasicText(
        text = title,
        style = Theme.typography.body.large.copy(color = Theme.colors.primary),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ShowcaseHint(text: String) {
    BasicText(
        text = text,
        style = Theme.typography.body.small.copy(color = Theme.colors.hint),
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
