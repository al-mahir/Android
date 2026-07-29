package com.iti.sheikh.presentation.availability

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.iti.sheikh.presentation.availability.AvailabilityToggleRow
import com.iti.sheikh.presentation.availability.BusyIndicator
import com.iti.sheikh.presentation.availability.IncomingRequestCard

@Composable
fun SheikhAvailabilityContent(
    state: AvailabilityUiState,
    onIntent: (AvailabilityIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                val transform: ContentTransform =
                    (fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.94f)) togetherWith
                        (fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.94f))
                transform
            },
            label = "availabilityState",
        ) { targetState ->
            when (targetState) {
                AvailabilityUiState.Offline -> AvailabilityToggleRow(
                    isAvailable = false,
                    onToggle = { onIntent(AvailabilityIntent.ToggleAvailability(true)) },
                )

                AvailabilityUiState.Available -> AvailabilityToggleRow(
                    isAvailable = true,
                    onToggle = { onIntent(AvailabilityIntent.ToggleAvailability(false)) },
                )

                is AvailabilityUiState.IncomingRequest -> IncomingRequestCard(
                    studentName = targetState.studentName,
                    note = targetState.note,
                    expiresAt = targetState.expiresAt,
                    onAccept = { onIntent(AvailabilityIntent.Accept) },
                    onDecline = { onIntent(AvailabilityIntent.Decline) },
                )

                AvailabilityUiState.Busy -> BusyIndicator()
            }
        }
    }
}
