package com.example.designsystem.components.mushaf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme

/**
 * A dismissible notice about the session itself.
 *
 * Used for things the service reports that change what the app can do — chiefly running a
 * different engine than the one asked for, which silently removes tajwīd grading. Those are
 * easy to swallow, and swallowing them leaves the reciter trusting feedback that was never
 * produced.
 */
@Composable
fun LiveNoticeBanner(
    text: String,
    dismissContentDescription: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.medium)
            .background(Theme.colors.amber.copy(alpha = 0.18f))
            .border(1.dp, Theme.colors.amber, Theme.shapes.medium)
            .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            // The text carries the meaning; a second announcement would be read out twice.
            contentDescription = null,
            tint = Theme.colors.onSurface,
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
        BasicText(
            text = text,
            style = Theme.typography.body.small.copy(color = Theme.colors.onSurface),
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = dismissContentDescription,
            tint = Theme.colors.onSurface,
            modifier = Modifier
                .size(Theme.size.iconSemiMedium)
                .clickable(onClick = onDismiss),
        )
    }
}

/** One place a heard passage might have come from. */
data class RecitationCandidateUi(
    val id: String,
    val reference: String,
    val text: String?,
)

/**
 * The passage occurs in more than one place; which one is being recited?
 *
 * Shown rather than guessed. Scoring someone against a verse they were not reciting is the worst
 * failure this system has available, so the service declines to place the passage and hands back
 * candidates instead — and a client that hides them turns a considered refusal into silence.
 *
 * Non-modal on purpose: the contract allows simply waiting for the next chunk to resolve it, so
 * this must not block a reciter who is still going.
 */
@Composable
fun RecitationCandidatesCard(
    title: String,
    candidates: List<RecitationCandidateUi>,
    dismissContentDescription: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.medium)
            .background(Theme.colors.surface)
            .border(1.dp, Theme.colors.border, Theme.shapes.medium)
            .padding(Theme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = title,
                style = Theme.typography.body.small.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = dismissContentDescription,
                tint = Theme.colors.secondaryFont,
                modifier = Modifier
                    .size(Theme.size.iconSemiMedium)
                    .clickable(onClick = onDismiss),
            )
        }

        candidates.forEach { candidate ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Theme.shapes.small)
                    .clickable { onSelect(candidate.id) }
                    .padding(vertical = Theme.spacing.extraSmall),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BasicText(
                    text = candidate.reference,
                    style = Theme.typography.body.small.copy(color = Theme.colors.primary),
                )
                // The verse text, because "(27, 30)" is a lookup the reciter would otherwise
                // have to perform themselves before they could answer the question.
                candidate.text?.takeIf { it.isNotBlank() }?.let { text ->
                    BasicText(
                        text = text,
                        style = Theme.typography.body.medium.copy(color = Theme.colors.primaryFont),
                    )
                }
            }
        }
    }
}
