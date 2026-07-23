package com.example.designsystem.components.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.theme.Theme

/**
 * The end-of-session result, shown without leaving the muṣḥaf.
 *
 * A sheet rather than a destination on purpose: the reciter has just finished reciting and the
 * next thing they usually want is to keep going. Navigating away to a results screen would make
 * every session end with a trip back.
 */
@Composable
fun SessionSummarySheet(
    headline: String,
    subtitle: String,
    stats: List<SessionStatUi>,
    breakdownTitle: String,
    breakdown: List<SessionBreakdownUi>,
    practiceTitle: String,
    practiceFocus: List<String>,
    emptyMessage: String?,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Theme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
        ) {
            SessionSummaryContent(
                headline = headline,
                subtitle = subtitle,
                stats = stats,
                breakdownTitle = breakdownTitle,
                breakdown = breakdown,
                practiceTitle = practiceTitle,
                practiceFocus = practiceFocus,
                emptyMessage = emptyMessage,
            )

            PrimaryButton(
                caption = dismissLabel,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
