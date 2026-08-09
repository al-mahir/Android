package com.iti.presentation.exam.summary.state

import com.iti.domain.model.exam.ExamMistake
import com.iti.domain.model.exam.ExamSummary

data class ExamSummaryUiState(
    val isLoading: Boolean = true,
    val summary: ExamSummary? = null,
    val showMistakesDialog: Boolean = false,
    val selectedMistake: ExamMistake? = null,
    val currentMistakeIndex: Int = 0, // Added to track current review step
) {
    val scoreGrade: String
        get() {
            val acc = summary?.accuracy ?: 0f
            return when {
                acc == 1.0f -> "ممتاز"
                acc >= 0.9f -> "جيد جداً"
                acc >= 0.75f -> "جيد"
                else -> "يحتاج مراجعة"
            }
        }
}