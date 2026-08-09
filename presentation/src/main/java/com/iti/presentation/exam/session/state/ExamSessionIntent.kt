package com.iti.presentation.exam.session.state

sealed interface ExamSessionIntent {
    object StartRecording : ExamSessionIntent
    object StopRecording : ExamSessionIntent
    object SkipQuestion : ExamSessionIntent

    object EndSessionClicked : ExamSessionIntent
    object EndSessionConfirmDismissed : ExamSessionIntent
    object EndSessionConfirmed : ExamSessionIntent
    object NextQuestionClicked : ExamSessionIntent
    object RevealNextWord : ExamSessionIntent
    object RevealFullHint : ExamSessionIntent
    object ToggleTrackerVisibility : ExamSessionIntent
    object ShowMistakesSheet : ExamSessionIntent
    object DismissMistakesSheet : ExamSessionIntent
    data class AudioPcmCaptured(val pcm16: ByteArray) : ExamSessionIntent
}
