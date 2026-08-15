package com.iti.presentation.exam.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.model.exam.ExamSummary
import com.iti.domain.usecase.exam.GetExamSummaryByIdUseCase
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.exam.summary.state.ExamSummaryEffect
import com.iti.presentation.exam.summary.state.ExamSummaryIntent
import com.iti.presentation.exam.summary.state.ExamSummaryUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 1. Create a Singleton Cache to pass the exact result in memory
object ExamSessionCache {
    var latestSummary: ExamSummary? = null
}

class ExamSummaryViewModel(
    private val summaryId: String,
    private val getSummaryById: GetExamSummaryByIdUseCase,
) : ViewModel(),
    StateHolder<ExamSummaryUiState> by DefaultStateHolder(ExamSummaryUiState()),
    EffectPublisher<ExamSummaryEffect> by DefaultEffectPublisher() {

    init {
        loadSummary()
    }

    private fun loadSummary() = viewModelScope.launch {
        updateState { copy(isLoading = true) }

        // 2. Try to get it directly from memory first (Bypasses Room serialization bugs completely!)
        val cached = ExamSessionCache.latestSummary
        if (cached != null && cached.id == summaryId) {
            updateState { copy(isLoading = false, summary = cached) }
            return@launch
        }

        // 3. Fallback to Database (e.g. if the user opens it later from History)
        var loadedSummary = getSummaryById(summaryId)
        var attempts = 0

        while (loadedSummary == null && attempts < 10) {
            delay(200)
            try {
                loadedSummary = getSummaryById(summaryId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            attempts++
        }

        updateState {
            copy(
                isLoading = false,
                summary = loadedSummary
            )
        }
    }

    fun onIntent(intent: ExamSummaryIntent) = when (intent) {
        ExamSummaryIntent.TestAgainClicked -> {
            sendEffect(ExamSummaryEffect.NavigateToSetup(currentState.summary?.scope))
        }
        ExamSummaryIntent.ReviewAllMistakesClicked -> {
            val safeMistakes = currentState.summary?.questionResults?.flatMap { it.mistakes } ?: emptyList()
            safeMistakes.firstOrNull()?.let {
                updateState { copy(showMistakesDialog = true, selectedMistake = it, currentMistakeIndex = 0) }
            }
        }
        is ExamSummaryIntent.MistakeClicked -> {
            val safeMistakes = currentState.summary?.questionResults?.flatMap { it.mistakes } ?: emptyList()
            val idx = safeMistakes.indexOf(intent.mistake).coerceAtLeast(0)
            updateState { copy(showMistakesDialog = true, selectedMistake = intent.mistake, currentMistakeIndex = idx) }
        }
        ExamSummaryIntent.NextMistakeClicked -> {
            val safeMistakes = currentState.summary?.questionResults?.flatMap { it.mistakes } ?: emptyList()
            val nextIdx = currentState.currentMistakeIndex + 1
            if (nextIdx < safeMistakes.size) {
                updateState { copy(selectedMistake = safeMistakes[nextIdx], currentMistakeIndex = nextIdx) }
            } else {
                updateState { copy(showMistakesDialog = false, selectedMistake = null) }
            }
        }
        ExamSummaryIntent.DismissMistakeDialog -> {
            updateState { copy(showMistakesDialog = false, selectedMistake = null) }
        }
        ExamSummaryIntent.BackClicked -> sendEffect(ExamSummaryEffect.NavigateBack)
    }
}