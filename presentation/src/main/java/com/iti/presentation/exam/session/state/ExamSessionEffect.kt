package com.iti.presentation.exam.session.state

sealed interface ExamSessionEffect {
    data class NavigateToSummary(val summaryId: String) : ExamSessionEffect
    object NavigateBack : ExamSessionEffect
    object RequestMicrophonePermission : ExamSessionEffect
    object PlayStopSound : ExamSessionEffect
}
