package com.iti.presentation.exam.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.card.InnerContentCard
import com.example.designsystem.components.dialog.ConfirmationDialog
import com.example.designsystem.components.section.SectionHeader
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.domain.model.exam.ExamMistake
import com.iti.domain.model.exam.ExamMistakeCategory
import com.iti.domain.model.exam.ExamScope
import com.iti.domain.model.exam.WordStatus
import com.iti.domain.model.quran.SurahNames
import com.iti.presentation.R
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.exam.summary.state.ExamSummaryEffect
import com.iti.presentation.exam.summary.state.ExamSummaryIntent
import com.iti.presentation.exam.summary.state.ExamSummaryUiState
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private val ColorCorrect = Color(0xFF22C55E)
private val ColorAlmost  = Color(0xFFF59E0B)
private val ColorError   = Color(0xFFEF4444)
private val ColorTrimmed = Color(0xFF94A3B8)

@Composable
fun ExamSummaryScreen(
    summaryId: String,
    viewModel: ExamSummaryViewModel = koinViewModel(parameters = { parametersOf(summaryId) }),
    onNavigateBack: () -> Unit,
    onNavigateToSetup: (ExamScope?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            ExamSummaryEffect.NavigateBack -> onNavigateBack()
            is ExamSummaryEffect.NavigateToSetup -> onNavigateToSetup(effect.scope)
        }
    }

    ExamSummaryContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}

@Composable
private fun ExamSummaryContent(
    state: ExamSummaryUiState,
    onIntent: (ExamSummaryIntent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackTitleTopBar(
                title = stringResource(R.string.exam_session_complete_title),
                onBackClick = { onIntent(ExamSummaryIntent.BackClicked) }
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Theme.colors.primary)
                }
            } else if (state.summary == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "لم يتم العثور على النتيجة",
                            style = Theme.typography.title,
                            color = Theme.colors.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SecondaryButton(
                            caption = "رجوع",
                            onClick = { onIntent(ExamSummaryIntent.BackClicked) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = Theme.spacing.extraLarge
                    )
                ) {
                    item {
                        ScoreHeader(state = state)
                    }

                    item {
                        StatsRow(state = state)
                    }

                    val hasValidRecitation = state.summary.questionResults.any { it.words.any { w -> w.status != WordStatus.PENDING } && !it.skipped }
                    val safeMistakes = state.summary.questionResults.flatMap { it.mistakes }

                    if (safeMistakes.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.exam_mistakes_to_review),
                                modifier = Modifier.padding(Theme.spacing.medium)
                            )
                        }

                        items(safeMistakes) { mistake ->
                            MistakeItem(mistake = mistake, onClick = { onIntent(ExamSummaryIntent.MistakeClicked(it)) })
                        }
                    } else if (hasValidRecitation) {
                        item {
                            InnerContentCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Theme.spacing.medium)
                            ) {
                                Text(
                                    text = stringResource(R.string.exam_no_mistakes),
                                    style = Theme.typography.body.large,
                                    color = Theme.colors.success,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Theme.spacing.large)
                                )
                            }
                        }
                    } else {
                        item {
                            InnerContentCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Theme.spacing.medium)
                            ) {
                                Text(
                                    text = stringResource(R.string.exam_summary_no_valid_recitation),
                                    style = Theme.typography.body.large,
                                    color = Theme.colors.hint,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Theme.spacing.large)
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Theme.spacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SecondaryButton(
                                caption = stringResource(R.string.exam_review_all_mistakes),
                                onClick = { onIntent(ExamSummaryIntent.ReviewAllMistakesClicked) },
                                isDisabled = safeMistakes.isEmpty(),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(Theme.spacing.medium))
                            PrimaryButton(
                                caption = stringResource(R.string.exam_test_again),
                                onClick = { onIntent(ExamSummaryIntent.TestAgainClicked) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (state.showMistakesDialog && state.selectedMistake != null) {
            val mistakes = state.summary?.questionResults?.flatMap { it.mistakes } ?: emptyList()
            val isLast = state.currentMistakeIndex >= mistakes.lastIndex
            val mistake = state.selectedMistake

            val said = if (!mistake.predictedText.isNullOrBlank()) mistake.predictedText else "—"
            val expected = mistake.word
            val typeLine = mistakeCategoryLabel(mistake.category)

            ConfirmationDialog(
                title = stringResource(R.string.exam_mistake_details_title_format, state.currentMistakeIndex + 1, mistakes.size),
                message = buildString {
                    appendLine("سورة ${SurahNames.nameOf(mistake.surahNumber) ?: ""} • الآية ${mistake.ayahNumber}")
                    appendLine()
                    appendLine("${stringResource(R.string.exam_mistake_expected)}: $expected")
                    appendLine("${stringResource(R.string.exam_mistake_said)}: $said")
                    append("${stringResource(R.string.exam_mistake_type)}: $typeLine")
                },
                confirmLabel = if (isLast) stringResource(R.string.exam_dialog_ok) else stringResource(R.string.exam_dialog_next),
                dismissLabel = stringResource(R.string.exam_dialog_close),
                onConfirm = {
                    if (isLast) onIntent(ExamSummaryIntent.DismissMistakeDialog)
                    else onIntent(ExamSummaryIntent.NextMistakeClicked)
                },
                onDismiss = { onIntent(ExamSummaryIntent.DismissMistakeDialog) }
            )
        }
    }
}

@Composable
private fun ScoreHeader(state: ExamSummaryUiState) {
    val totalWords = state.summary?.questionResults?.sumOf { it.words.size } ?: 1
    val correctWords = state.summary?.questionResults?.sumOf { it.words.count { w -> w.status == WordStatus.CORRECT } } ?: 0
    val computedAccuracy = if (totalWords > 0) correctWords.toFloat() / totalWords else 0f

    val percentage = (computedAccuracy * 100).toInt()
    val scoreText = when {
        computedAccuracy == 1.0f -> stringResource(R.string.exam_score_well_done)
        computedAccuracy >= 0.75f -> stringResource(R.string.exam_score_good)
        else -> stringResource(R.string.exam_score_keep_practicing)
    }

    val ringColor = when {
        computedAccuracy >= 0.9f -> Theme.colors.success
        computedAccuracy >= 0.75f -> Theme.colors.primary
        else -> Theme.colors.warning
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Theme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(140.dp),
                color = ringColor.copy(alpha = 0.12f),
                strokeWidth = 10.dp,
            )
            CircularProgressIndicator(
                progress = { computedAccuracy },
                modifier = Modifier.size(140.dp),
                color = ringColor,
                strokeWidth = 10.dp,
            )
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(ringColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage%",
                        style = Theme.typography.display.copy(fontWeight = FontWeight.Bold),
                        color = ringColor,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Theme.spacing.medium))

        Text(
            text = scoreText,
            style = Theme.typography.title,
            color = Theme.colors.onSurface,
        )
        val scoreGrade = try { state.scoreGrade } catch (e: Exception) { "" }
        if (scoreGrade.isNotEmpty()) {
            Text(
                text = scoreGrade,
                style = Theme.typography.body.large,
                color = Theme.colors.hint,
            )
        }
    }
}

@Composable
private fun StatsRow(state: ExamSummaryUiState) {
    val summary = state.summary ?: return

    val durationSeconds = summary.totalDurationMs / 1000
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedDuration = "%02d:%02d".format(minutes, seconds)

    // Explicit dynamic calculation
    val dynamicMistakeCount = summary.questionResults.flatMap { it.mistakes }.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            value = summary.totalQuestions.toString(),
            label = stringResource(R.string.exam_stat_questions)
        )
        StatItem(
            value = dynamicMistakeCount.toString(),
            label = stringResource(R.string.exam_stat_mistakes),
            color = if (dynamicMistakeCount > 0) Theme.colors.error else Theme.colors.onSurface
        )
        StatItem(
            value = formattedDuration,
            label = stringResource(R.string.exam_stat_duration)
        )
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color = Theme.colors.onSurface) {
    InnerContentCard(modifier = Modifier.padding(Theme.spacing.small)) {
        Column(
            modifier = Modifier.padding(Theme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = Theme.typography.title,
                color = color
            )
            Text(
                text = label,
                style = Theme.typography.body.small,
                color = Theme.colors.hint
            )
        }
    }
}

@Composable
private fun MistakeItem(mistake: ExamMistake, onClick: (ExamMistake) -> Unit) {
    InnerContentCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.extraSmall)
            .clickable { onClick(mistake) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
        ) {
            Text(
                text = "سورة ${SurahNames.nameOf(mistake.surahNumber) ?: ""} • الآية ${mistake.ayahNumber}",
                style = Theme.typography.body.small,
                color = Theme.colors.hint,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.exam_mistake_expected),
                        style = Theme.typography.body.small,
                        color = Theme.colors.hint,
                    )
                    Text(
                        text = mistake.word,
                        style = Theme.typography.body.large.copy(fontWeight = FontWeight.Bold),
                        color = Theme.colors.onSurface,
                    )
                }

                val predictedWord = mistake.predictedText
                if (!predictedWord.isNullOrBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.exam_mistake_said),
                            style = Theme.typography.body.small,
                            color = Theme.colors.hint,
                        )
                        Text(
                            text = predictedWord,
                            style = Theme.typography.body.large.copy(fontWeight = FontWeight.Bold),
                            color = ColorError,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        mistakeCategoryColor(mistake.category).copy(alpha = 0.12f),
                        Theme.shapes.small,
                    )
                    .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.extraSmall)
            ) {
                Text(
                    text = mistakeCategoryLabel(mistake.category),
                    style = Theme.typography.body.small.copy(fontWeight = FontWeight.SemiBold),
                    color = mistakeCategoryColor(mistake.category),
                )
            }
        }
    }
}

@Composable
private fun mistakeCategoryLabel(category: ExamMistakeCategory): String = when (category) {
    ExamMistakeCategory.MEMORIZATION -> stringResource(R.string.exam_mistake_category_memorization)
    ExamMistakeCategory.TASHKEEL    -> stringResource(R.string.exam_mistake_category_tashkeel)
    ExamMistakeCategory.TAJWEED     -> stringResource(R.string.exam_mistake_category_tajweed)
    ExamMistakeCategory.OTHER       -> stringResource(R.string.exam_mistake_category_other)
}

private fun mistakeCategoryColor(category: ExamMistakeCategory): Color = when (category) {
    ExamMistakeCategory.MEMORIZATION -> ColorError
    ExamMistakeCategory.TASHKEEL    -> ColorAlmost
    ExamMistakeCategory.TAJWEED     -> Color(0xFF8B5CF6)
    ExamMistakeCategory.OTHER       -> ColorTrimmed
}