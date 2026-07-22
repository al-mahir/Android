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




 
private fun LiveCorrectionUiState.statusLabelRes(isRecording: Boolean): Int? = when {
    !isRecording -> null
    isConnecting -> R.string.mushaf_live_connecting
    !isActive -> R.string.mushaf_live_starting
    
    lastOutcome == ChunkOutcome.NO_MATCH -> R.string.mushaf_live_no_match
    lastOutcome == ChunkOutcome.AMBIGUOUS -> R.string.mushaf_live_ambiguous_short
    
    wordFeedback.isEmpty() -> R.string.mushaf_live_listening
    else -> null
}
