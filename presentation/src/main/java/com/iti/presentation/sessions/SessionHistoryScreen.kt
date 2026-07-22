package com.iti.presentation.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.components.placeholderscreens.EmptyDataScreen
import com.example.designsystem.components.session.SessionSummaryContent
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.domain.model.recitation.RecitationSessionSummary
import com.iti.presentation.R
import org.koin.androidx.compose.koinViewModel

/**
 * The reciter's past sessions.
 *
 * Stateful wrapper: collects state and delegates to a stateless content, per AGENTS.md §3a.
 */
@Composable
fun SessionHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SessionHistoryContent(
        state = state,
        onBack = onBack,
        onSelect = { viewModel.onIntent(SessionHistoryIntent.Select(it)) },
        onDelete = { viewModel.onIntent(SessionHistoryIntent.Delete(it)) },
        modifier = modifier,
    )
}

@Composable
internal fun SessionHistoryContent(
    state: SessionHistoryUiState,
    onBack: () -> Unit,
    onSelect: (String?) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = rememberLocale()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.session_history_title),
            onBackClick = onBack,
        )

        when {
            state.isEmpty -> EmptyDataScreen(
                modifier = Modifier.fillMaxSize(),
                description = stringResource(R.string.session_history_empty),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Theme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                items(state.sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        dateLabel = session.dateLabel(locale),
                        onClick = { onSelect(session.id) },
                    )
                }
            }
        }
    }

    state.selected?.let { session ->
        SessionDetailSheet(
            session = session,
            onDelete = { onDelete(session.id) },
            onDismiss = { onSelect(null) },
        )
    }
}

@Composable
private fun SessionRow(
    session: RecitationSessionSummary,
    dateLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.medium)
            .border(1.dp, Theme.colors.border, Theme.shapes.medium)
            .background(Theme.colors.surface)
            .clickable(onClick = onClick)
            .padding(Theme.spacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = session.rangeLabel(),
                style = Theme.typography.body.medium.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            BasicText(
                text = dateLabel,
                style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
            )
        }

        // The row's headline figure is the mistake count, not accuracy: a count is a fact the
        // service asserted, while the percentage rests on thresholds documented as uncalibrated.
        Box(
            modifier = Modifier
                .clip(Theme.shapes.circle)
                .background(
                    if (session.mistakeCount == 0) Theme.colors.success else Theme.colors.error,
                )
                .padding(horizontal = Theme.spacing.small, vertical = 2.dp),
        ) {
            BasicText(
                text = session.mistakeCount.toString(),
                style = Theme.typography.body.small.copy(
                    color = if (session.mistakeCount == 0) Theme.colors.onSuccess else Theme.colors.onError,
                ),
            )
        }
    }
}

@Composable
private fun SessionDetailSheet(
    session: RecitationSessionSummary,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = rememberLocale()

    AppBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = Theme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
        ) {
            SessionSummaryContent(
                headline = session.headline(),
                subtitle = "${session.rangeLabel()} · ${session.dateLabel(locale)}",
                stats = session.stats(locale),
                breakdownTitle = stringResource(R.string.session_breakdown_title),
                breakdown = session.breakdown(),
                practiceTitle = stringResource(R.string.session_practice_title),
                practiceFocus = session.practiceLines(),
                emptyMessage = session.emptyMessageOrNull(),
            )

            if (session.mistakes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
                    BasicText(
                        text = stringResource(R.string.session_mistakes_title),
                        style = Theme.typography.body.medium.copy(
                            color = Theme.colors.primaryFont,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    session.mistakes.forEach { mistake ->
                        BasicText(
                            text = stringResource(
                                R.string.session_mistake_row,
                                mistake.word,
                                mistake.sura,
                                mistake.aya,
                            ),
                            style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                        )
                    }
                }
            }

            BasicText(
                text = stringResource(R.string.session_delete),
                style = Theme.typography.body.medium.copy(color = Theme.colors.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Theme.shapes.medium)
                    .clickable(onClick = onDelete)
                    .padding(Theme.spacing.medium),
            )
        }
    }
}
