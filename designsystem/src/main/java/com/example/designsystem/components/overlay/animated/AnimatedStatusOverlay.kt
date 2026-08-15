package com.example.designsystem.components.overlay.animated

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.LottieConstants
import com.example.designsystem.R
import com.example.designsystem.components.utils.LottiePlayer
import com.example.designsystem.theme.Theme
import kotlinx.coroutines.delay

@Composable
internal fun StatusContainer(
    modifier: Modifier = Modifier,
    contentDescription: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val minSize = Theme.size.overlayContainer
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = minSize, minHeight = minSize)
            .background(color = Theme.colors.backGround, shape = Theme.shapes.large)
            .padding(Theme.spacing.medium)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small, Alignment.CenterVertically),
        content = content,
    )
}

/**
 * Renders an animated status overlay over the current screen. Pass `null`
 * (the default) to show nothing.
 *
 * Behavior:
 *  - `Loading` blocks back-press and tap-outside dismissal; back-press
 *    triggers a haptic so the user knows the press registered.
 *  - `Success` / `Error` auto-dismiss when the Lottie animation finishes.
 *    A backstop timer ([autoDismissTimeoutMillis]) guarantees they don't
 *    stick if the animation callback never fires.
 *  - The optional `message` on `Success` / `Error` is rendered below the
 *    animation and announced by accessibility services in place of the
 *    generic state label.
 */
@Composable
fun StatusOverlay(
    state: OverlayState?,
    onDismiss: () -> Unit,
    autoDismissTimeoutMillis: Long = 5_000L,
) {
    if (state == null) return

    val haptic = LocalHapticFeedback.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val description = state.contentDescription()

    if (state !is OverlayState.Loading) {
        LaunchedEffect(state) {
            delay(autoDismissTimeoutMillis)
            currentOnDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (state is OverlayState.Loading) {
                // Back was pressed (or click-outside reached us — though it can't
                // when `dismissOnClickOutside = false`). Acknowledge the input
                // without dismissing.
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else {
                currentOnDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = state !is OverlayState.Loading,
        ),
    ) {
        StatusContainer(contentDescription = description) {
            when (state) {
                OverlayState.Loading -> LottiePlayer(
                    resId = R.raw.anim_loading,
                    iterations = LottieConstants.IterateForever,
                    size = Theme.size.overlayIndicator,
                )
                is OverlayState.Success -> {
                    LottiePlayer(
                        resId = R.raw.anim_success,
                        iterations = 1,
                        size = Theme.size.overlayIndicator,
                        onAnimationFinished = currentOnDismiss,
                    )
                    OverlayMessage(state.message)
                }
                is OverlayState.Error -> {
                    LottiePlayer(
                        resId = R.raw.anim_error,
                        iterations = 1,
                        size = Theme.size.overlayIndicator,
                    )
                    OverlayMessage(state.message)
                }
            }
        }
    }
}

@Composable
private fun OverlayMessage(message: String?) {
    if (message.isNullOrBlank()) return
    BasicText(
        text = message,
        style = Theme.typography.body.medium.copy(
            color = Theme.colors.primaryFont,
            textAlign = TextAlign.Center,
        ),
    )
}

@Composable
private fun OverlayState.contentDescription(): String = when (this) {
    OverlayState.Loading -> stringResource(R.string.overlay_status_loading)
    is OverlayState.Success -> message ?: stringResource(R.string.overlay_status_success)
    is OverlayState.Error -> message ?: stringResource(R.string.overlay_status_error)
}
