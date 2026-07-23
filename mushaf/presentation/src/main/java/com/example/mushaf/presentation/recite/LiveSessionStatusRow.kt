package com.example.mushaf.presentation.recite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.designsystem.components.mushaf.AccuracyPill
import com.example.designsystem.components.mushaf.CorrectionsBadge
import com.example.designsystem.components.mushaf.LiveNoticeBanner
import com.example.designsystem.components.mushaf.LiveStatusPill
import com.example.designsystem.components.mushaf.RecitationCandidateUi
import com.example.designsystem.components.mushaf.RecitationCandidatesCard
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.recite.NonVerseSegment
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.state.ChunkOutcome
import com.example.mushaf.presentation.state.LiveCorrectionUiState


@Composable
fun LiveSessionStatusRow(
    live: LiveCorrectionUiState,
    isRecording: Boolean,
    onCorrectionsClick: () -> Unit,
    onDismissEngineNotice: () -> Unit,
    onSelectCandidate: (RecitationCursor) -> Unit,
    onDismissCandidates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        
        
        if (live.engineSubstituted) {
            LiveNoticeBanner(
                text = stringResource(R.string.mushaf_live_engine_notice, live.engine.orEmpty()),
                dismissContentDescription = stringResource(R.string.mushaf_cd_dismiss_notice),
                onDismiss = onDismissEngineNotice,
            )
        }

        if (live.lastOutcome == ChunkOutcome.AMBIGUOUS && live.candidates.isNotEmpty()) {
            RecitationCandidatesCard(
                title = stringResource(R.string.mushaf_live_ambiguous),
                candidates = live.candidates.map { candidate ->
                    RecitationCandidateUi(
                        id = candidate.start.wordId,
                        reference = stringResource(
                            R.string.mushaf_ayah_label,
                            candidate.start.sura,
                            candidate.start.aya,
                        ),
                        text = candidate.text,
                    )
                },
                dismissContentDescription = stringResource(R.string.mushaf_cd_dismiss_notice),
                onSelect = { wordId -> RecitationCursor.fromWordId(wordId)?.let(onSelectCandidate) },
                onDismiss = onDismissCandidates,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
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

            
            
            live.accuracy?.let { accuracy ->
                AccuracyPill(
                    accuracy = accuracy,
                    scoredWordCount = live.scoredWordCount,
                    contentDescription = stringResource(R.string.mushaf_cd_accuracy),
                )
            }

            
            
            live.nonVerse.forEach { segment ->
                LiveStatusPill(label = stringResource(segment.labelRes()))
            }

            live.statusLabelRes(isRecording)?.let { labelRes ->
                LiveStatusPill(label = stringResource(labelRes))
            }
        }
    }
}

private fun NonVerseSegment.labelRes(): Int = when (this) {
    NonVerseSegment.ISTIAATHA -> R.string.mushaf_non_verse_istiaatha
    NonVerseSegment.BASMALAH -> R.string.mushaf_non_verse_basmalah
    NonVerseSegment.SADAKA -> R.string.mushaf_non_verse_sadaka
    NonVerseSegment.OTHER -> R.string.mushaf_non_verse_other
}



 
private fun LiveCorrectionUiState.statusLabelRes(isRecording: Boolean): Int? = when {
    !isRecording -> null
    isConnecting -> R.string.mushaf_live_connecting
    !isActive -> R.string.mushaf_live_starting
    lastOutcome == ChunkOutcome.NO_MATCH -> R.string.mushaf_live_no_match
    
    lastOutcome == ChunkOutcome.AMBIGUOUS && candidates.isEmpty() ->
        R.string.mushaf_live_ambiguous_short
    
    wordFeedback.isEmpty() -> R.string.mushaf_live_listening
    else -> null
}
