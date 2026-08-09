package com.iti.presentation.exam.summary.state

import com.iti.domain.model.exam.ExamScope

sealed interface ExamSummaryEffect {
    object NavigateBack : ExamSummaryEffect
    data class NavigateToSetup(val scope: ExamScope? = null) : ExamSummaryEffect
}
