package com.iti.presentation.exam.summary.state

import com.iti.domain.model.exam.ExamMistake

sealed interface ExamSummaryIntent {
    object TestAgainClicked : ExamSummaryIntent
    object ReviewAllMistakesClicked : ExamSummaryIntent
    data class MistakeClicked(val mistake: ExamMistake) : ExamSummaryIntent
    object NextMistakeClicked : ExamSummaryIntent
    object DismissMistakeDialog : ExamSummaryIntent
    object BackClicked : ExamSummaryIntent
}