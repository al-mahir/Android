package com.example.designsystem.components.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme

/** One headline number. */
data class SessionStatUi(
    val value: String,
    val label: String,
)

/** One bar of the mistake breakdown. */
data class SessionBreakdownUi(
    val label: String,
    val count: Int,
    val color: Color,
)

/**
 * What a recitation session amounted to.
 *
 * Shared between the muṣḥaf (shown the moment a session ends) and the profile's history, so the
 * reciter reads the same summary in both places rather than two views that drift apart.
 *
 * Takes resolved strings and numbers only — it knows nothing about the AI protocol, and it is not
 * responsible for deciding what counts as a mistake. That filtering happens before it.
 *
 * @param headline the framing sentence — an encouragement, not a grade.
 * @param stats up to four figures. Accuracy belongs here only where the caller is willing to
 *   stand behind it; the underlying thresholds are documented upstream as uncalibrated.
 * @param emptyMessage shown when the session graded nothing, which is a connection problem and
 *   must not be rendered as a flawless recitation.
 */
@Composable
fun SessionSummaryContent(
    headline: String,
    subtitle: String,
    stats: List<SessionStatUi>,
    breakdownTitle: String,
    breakdown: List<SessionBreakdownUi>,
    practiceTitle: String,
    practiceFocus: List<String>,
    emptyMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = headline,
                style = Theme.typography.title.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = subtitle,
                style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
            )
        }

        if (emptyMessage != null) {
            BasicText(
                text = emptyMessage,
                style = Theme.typography.body.medium.copy(
                    color = Theme.colors.secondaryFont,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Theme.shapes.medium)
                    .background(Theme.colors.surfaceVariant)
                    .padding(Theme.spacing.large),
            )
            return@Column
        }

        if (stats.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                stats.forEach { stat ->
                    StatTile(stat = stat, modifier = Modifier.weight(1f))
                }
            }
        }

        if (breakdown.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                BasicText(
                    text = breakdownTitle,
                    style = Theme.typography.body.medium.copy(
                        color = Theme.colors.primaryFont,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                val total = breakdown.sumOf { it.count }.coerceAtLeast(1)
                breakdown.forEach { entry ->
                    BreakdownBar(entry = entry, total = total)
                }
            }
        }

        if (practiceFocus.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Theme.shapes.medium)
                    .background(Theme.colors.primaryContainer)
                    .padding(Theme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
            ) {
                BasicText(
                    text = practiceTitle,
                    style = Theme.typography.body.medium.copy(
                        color = Theme.colors.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                practiceFocus.forEach { line ->
                    BasicText(
                        text = line,
                        style = Theme.typography.body.small.copy(color = Theme.colors.onPrimaryContainer),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(stat: SessionStatUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(Theme.shapes.medium)
            .border(1.dp, Theme.colors.border, Theme.shapes.medium)
            .background(Theme.colors.surface)
            .padding(vertical = Theme.spacing.medium, horizontal = Theme.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            text = stat.value,
            style = Theme.typography.body.large.copy(
                color = Theme.colors.primaryFont,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
        BasicText(
            text = stat.label,
            style = Theme.typography.body.small.copy(
                color = Theme.colors.secondaryFont,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/**
 * A labelled proportional bar.
 *
 * The count is printed beside it rather than left to the bar's length: a bar alone is a shape,
 * and "how many" is the thing a reciter is trying to read.
 */
@Composable
private fun BreakdownBar(entry: SessionBreakdownUi, total: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(
                text = entry.label,
                style = Theme.typography.body.small.copy(color = Theme.colors.primaryFont),
            )
            BasicText(
                text = entry.count.toString(),
                style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(Theme.shapes.circle)
                .background(Theme.colors.surfaceVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(entry.count.toFloat() / total)
                    .height(6.dp)
                    .background(entry.color),
            ) {}
        }
    }
}
