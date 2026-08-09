package com.iti.presentation.exam.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.model.exam.ExamScope
import com.iti.domain.usecase.exam.ComputeQuestionCountRangeUseCase
import com.iti.domain.usecase.exam.GetRecentExamScopesUseCase
import com.iti.domain.repository.ExamRepository
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.exam.setup.state.ExamSetupEffect
import com.iti.presentation.exam.setup.state.ExamSetupIntent
import com.iti.presentation.exam.setup.state.ExamSetupUiState
import com.iti.presentation.exam.setup.state.SetupTab
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class ExamSetupViewModel(
    getRecentScopes: GetRecentExamScopesUseCase,
    private val computeRange: ComputeQuestionCountRangeUseCase,
    private val examRepository: ExamRepository,
    initialScope: ExamScope? = null,
) : ViewModel(),
    StateHolder<ExamSetupUiState> by DefaultStateHolder(ExamSetupUiState()),
    EffectPublisher<ExamSetupEffect> by DefaultEffectPublisher() {

    init {
        if (initialScope != null) {
            applyScope(initialScope)
        }

        getRecentScopes()
            .onEach { scopes -> updateState { copy(recentScopes = scopes) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val customScopes = examRepository.getCustomScopes()
            updateState { copy(customScopes = customScopes) }

            // Auto-select the first custom scope if available
            if (customScopes.isNotEmpty() && currentState.activeScope == null) {
                val first = customScopes.first()
                updateState { copy(selectedScopeId = first.id, activeScope = first) }
                recomputeRange(currentState, first)
            }
        }
    }

    fun onIntent(intent: ExamSetupIntent) {
        when (intent) {
            ExamSetupIntent.ToggleCreateRangeView -> {
                val isCreating = !currentState.isCreatingNewRange
                updateState {
                    copy(
                        isCreatingNewRange = isCreating,
                        customScopeComponents = emptyList(), // Reset builder on toggle
                        customScopeName = ""
                    )
                }
                recomputeRange(currentState, if (isCreating) null else currentState.activeScope)
            }
            is ExamSetupIntent.ScopeSelected -> {
                updateState { copy(selectedScopeId = intent.id, activeScope = intent.scope) }
                recomputeRange(currentState, intent.scope)
            }
            is ExamSetupIntent.TabSelected -> {
                updateState { copy(currentTab = intent.tab) }
                recomputeRange(currentState, null)
            }
            is ExamSetupIntent.SurahSelected -> {
                updateState { copy(selectedSurah = intent.surahNumber) }
                recomputeRange(currentState, null)
            }
            is ExamSetupIntent.JuzSelected -> {
                updateState { copy(selectedJuz = intent.juzNumber) }
                recomputeRange(currentState, null)
            }
            is ExamSetupIntent.RubSelected -> {
                updateState { copy(selectedRub = intent.rubNumber) }
                recomputeRange(currentState, null)
            }
            is ExamSetupIntent.AyahRangeSelected -> {
                updateState { copy(selectedAyahRange = intent.start to intent.end) }
                recomputeRange(currentState, null)
            }
            is ExamSetupIntent.QuestionCountChanged -> {
                updateState { copy(selectedQuestionCount = intent.count) }
            }
            is ExamSetupIntent.LinesCountChanged -> {
                updateState { copy(selectedLinesPerQuestion = intent.count) }
            }
            is ExamSetupIntent.CustomScopeNameChanged -> {
                updateState { copy(customScopeName = intent.name) }
            }
            ExamSetupIntent.AddCurrentScopeToCustomClicked -> {
                val current = buildCurrentScope(currentState)
                val newComponents = currentState.customScopeComponents + current
                updateState { copy(customScopeComponents = newComponents) }
                recomputeRange(currentState.copy(customScopeComponents = newComponents), null)
            }
            is ExamSetupIntent.RemoveScopeFromCustomClicked -> {
                val current = currentState.customScopeComponents.toMutableList()
                if (intent.index in current.indices) {
                    current.removeAt(intent.index)
                    val newComponents = current.toList()
                    updateState { copy(customScopeComponents = newComponents) }
                    recomputeRange(currentState.copy(customScopeComponents = newComponents), null)
                }
            }
            ExamSetupIntent.SaveCustomScopeClicked -> {
                val name = currentState.customScopeName.ifBlank { "نطاق ${currentState.customScopes.size + 1}" }

                // If they didn't add anything to the list yet, use what's currently selected in the UI
                val components = currentState.customScopeComponents.ifEmpty {
                    listOf(buildCurrentScope(currentState))
                }

                if (components.isEmpty()) return

                val newScope = ExamScope.CustomRange(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    components = components
                )

                viewModelScope.launch {
                    examRepository.saveCustomScope(newScope)
                    val customScopes = examRepository.getCustomScopes()
                    updateState {
                        copy(
                            customScopes = customScopes,
                            customScopeName = "",
                            customScopeComponents = emptyList(),
                            isCreatingNewRange = false, // Go back to dashboard
                            selectedScopeId = newScope.id,
                            activeScope = newScope
                        )
                    }
                    recomputeRange(currentState, newScope)
                }
            }
            is ExamSetupIntent.DeleteCustomScopeClicked -> {
                viewModelScope.launch {
                    examRepository.deleteCustomScope(intent.id)
                    val customScopes = examRepository.getCustomScopes()
                    updateState { copy(customScopes = customScopes) }
                }
            }
            is ExamSetupIntent.RecentExamHistoryClicked -> {
                sendEffect(ExamSetupEffect.NavigateToSummary(intent.summaryId))
            }
            ExamSetupIntent.StartClicked -> {
                val scope = currentState.activeScope
                val count = currentState.selectedQuestionCount
                val lines = currentState.selectedLinesPerQuestion
                if (currentState.canStart && scope != null) {
                    sendEffect(ExamSetupEffect.NavigateToSession(scope, count, lines))
                }
            }
            ExamSetupIntent.BackClicked -> {
                if (currentState.isCreatingNewRange) {
                    updateState { copy(isCreatingNewRange = false) }
                    recomputeRange(currentState, currentState.activeScope)
                } else {
                    sendEffect(ExamSetupEffect.NavigateBack)
                }
            }
        }
    }

    private fun recomputeRange(state: ExamSetupUiState, scopeOverride: ExamScope?) {
        val scope = scopeOverride ?: if (state.isCreatingNewRange && state.customScopeComponents.isNotEmpty()) {
            ExamScope.CustomRange(UUID.randomUUID().toString(), "Temp", state.customScopeComponents)
        } else {
            buildCurrentScope(state)
        }

        val range = computeRange(scope)
        updateState {
            copy(
                questionCountRange = range,
                selectedQuestionCount = if (range.isEmpty()) 3 else selectedQuestionCount.coerceIn(range),
            )
        }
    }

    private fun applyScope(scope: ExamScope) {
        updateState { copy(activeScope = scope) }
        recomputeRange(currentState, scope)
    }

    private fun buildCurrentScope(state: ExamSetupUiState): ExamScope = when (state.currentTab) {
        SetupTab.SURAH -> ExamScope.SingleSurah(state.selectedSurah)
        SetupTab.JUZ -> ExamScope.SingleJuz(state.selectedJuz)
        SetupTab.RUB -> ExamScope.SingleRub(state.selectedRub)
        SetupTab.AYAH -> ExamScope.AyahRange(
            startSurah = state.selectedAyahRange.first.first,
            startAyah = state.selectedAyahRange.first.second,
            endSurah = state.selectedAyahRange.second.first,
            endAyah = state.selectedAyahRange.second.second,
        )
    }
}