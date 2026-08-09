package com.iti.presentation.exam.setup.state

import com.iti.domain.model.exam.ExamScope

sealed interface ExamSetupIntent {
    // Navigation & View Toggles
    object ToggleCreateRangeView : ExamSetupIntent
    object BackClicked : ExamSetupIntent
    object StartClicked : ExamSetupIntent

    // Dashboard Selections
    data class QuestionCountChanged(val count: Int) : ExamSetupIntent
    data class LinesCountChanged(val count: Int) : ExamSetupIntent
    data class ScopeSelected(val id: String, val scope: ExamScope) : ExamSetupIntent
    data class RecentExamHistoryClicked(val summaryId: String) : ExamSetupIntent
    data class DeleteCustomScopeClicked(val id: String) : ExamSetupIntent

    // Range Creation Builder
    data class TabSelected(val tab: SetupTab) : ExamSetupIntent
    data class SurahSelected(val surahNumber: Int) : ExamSetupIntent
    data class JuzSelected(val juzNumber: Int) : ExamSetupIntent
    data class RubSelected(val rubNumber: Int) : ExamSetupIntent
    data class AyahRangeSelected(val start: Pair<Int, Int>, val end: Pair<Int, Int>) : ExamSetupIntent
    data class CustomScopeNameChanged(val name: String) : ExamSetupIntent
    object AddCurrentScopeToCustomClicked : ExamSetupIntent
    data class RemoveScopeFromCustomClicked(val index: Int) : ExamSetupIntent
    object SaveCustomScopeClicked : ExamSetupIntent
}