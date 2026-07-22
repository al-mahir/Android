package com.example.mushaf.presentation.recite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.mushaf.AccuracyPill
import com.example.designsystem.components.mushaf.CorrectionsBadge
import com.example.designsystem.components.mushaf.LiveStatusPill
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.state.ChunkOutcome
import com.example.mushaf.presentation.state.LiveCorrectionUiState

/**
 * The live session's status line: what the session is doing, how it is going, and a way in to
 * the mistakes.
 *
 * A correction session is silent by design — no mistakes means no marks on the page. But a
 * session that never connected also produces no marks, and the two must not look identical: a
 * reciter reading silence as "flawless" is the failure mode this row exists to prevent.
 */
@Composable
fun LiveSessionStatusRow(
    live: LiveCorrectionUiState,
    isRecording: Boolean,
    onCorrectionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (live.mistakeCount > 0) {
            CorrectionsBadge(
                count = live.mistakeCount,
                contentDescription = pluralStringResource(
                    R.plurals.mushaf_cd_corrections,
                    live.mistakeCount,
                    live.mistakeCount,
                ),
                onClick = onCorrectionsClick,
            )
        }

        // Only once something has actually been scored. A pill reading 100% before a single word
        // was graded would be an assertion about a recitation nobody has checked.
        live.accuracy?.let { accuracy ->
            AccuracyPill(
                accuracy = accuracy,
                scoredWordCount = live.scoredWordCount,
                contentDescription = stringResource(R.string.mushaf_cd_accuracy),
            )
        }

        live.statusLabelRes(isRecording)?.let { labelRes ->
            LiveStatusPill(label = stringResource(labelRes))
        }
    }
}

/**
 * The one-line answer to "is anything happening?", or null when the marks on the page already
 * say it.
 */
private fun LiveCorrectionUiState.statusLabelRes(isRecording: Boolean): Int? = when {
    !isRecording -> null
    isConnecting -> R.string.mushaf_live_connecting
    !isActive -> R.string.mushaf_live_starting
    // The service declined to place what it heard. Say so — silence would be read as approval.
    lastOutcome == ChunkOutcome.NO_MATCH -> R.string.mushaf_live_no_match
    lastOutcome == ChunkOutcome.AMBIGUOUS -> R.string.mushaf_live_ambiguous_short
    // Nothing graded yet on a live session: confirm it is listening rather than stalled.
    wordFeedback.isEmpty() -> R.string.mushaf_live_listening
    else -> null
}
