package com.iti.presentation.exam.setup.state

import com.iti.domain.model.exam.ExamScope


sealed interface ExamSetupEffect {
    object NavigateBack : ExamSetupEffect
    data class NavigateToSession(
        val scope: ExamScope,
        val questionCount: Int,
        val linesPerQuestion: Int
    ) : ExamSetupEffect
    data class NavigateToSummary(val summaryId: String) : ExamSetupEffect
}
