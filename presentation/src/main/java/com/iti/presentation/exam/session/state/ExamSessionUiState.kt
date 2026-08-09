package com.iti.presentation.exam.session.state

import com.iti.domain.model.exam.ExamQuestion
import com.iti.domain.model.exam.ExamQuestionResult
import com.iti.domain.model.exam.ExamScope
import com.iti.domain.model.exam.QuestionStatus
import com.iti.domain.repository.exam.FeedbackCandidate

data class ExamSessionUiState(
    val scope: ExamScope = ExamScope.SingleSurah(1),
    val totalQuestions: Int = 3,
    val isPreparing: Boolean = true,
    val questions: List<ExamQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val activeQuestionResult: ExamQuestionResult? = null,
    val currentRecitationSurah: Int? = null,
    val currentRecitationAyah: Int? = null,
    val currentRecitationWord: Int? = null, // Fixes 'currentRecitationWord' error
    val isTrackerVisible: Boolean = true,
    val questionStatuses: List<QuestionStatus> = emptyList(),
    val isRecording: Boolean = false,
    val isConnecting: Boolean = false,
    val hasConnectionError: Boolean = false,
    val showEndConfirmDialog: Boolean = false,
    val lastFeedbackStatus: String = "ok",
    val candidates: List<FeedbackCandidate> = emptyList(),
    val elapsedSeconds: Int = 0,
    val isReviewing: Boolean = false,
    val showHasbuk: Boolean = false,
    val countdownSeconds: Int? = null,
    val hintWords: List<String> = emptyList(),
    val highlightedHintWords: Int = 0,
    val revealedWordsCount: Int = 0,
    val showFullHint: Boolean = false,
    val showMistakesSheet: Boolean = false,
) {
    val progressPercent: Float
        get() = if (totalQuestions == 0) 0f else currentQuestionIndex.toFloat() / totalQuestions

    val currentQuestion: ExamQuestion?
        get() = questions.getOrNull(currentQuestionIndex)

    val displayMistakeCount: Int
        get() = activeQuestionResult?.mistakes?.size ?: 0

    val allQuestionStatuses: List<QuestionStatus>
        get() {
            val completed = questionStatuses
            val remaining = List((totalQuestions - completed.size).coerceAtLeast(0)) { QuestionStatus.PENDING }
            return completed + remaining
        }
}