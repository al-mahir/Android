package com.iti.presentation.exam.setup.state

import com.iti.domain.model.exam.ExamScope
import com.iti.domain.model.exam.RecentExamScope

data class ExamSetupUiState(
    // View State
    val isCreatingNewRange: Boolean = false,

    // Dashboard State
    val selectedScopeId: String? = null,
    val activeScope: ExamScope? = null,
    val questionCountRange: IntRange = IntRange.EMPTY,
    val selectedQuestionCount: Int = 3,
    val selectedLinesPerQuestion: Int = 3,
    val recentScopes: List<RecentExamScope> = emptyList(),
    val customScopes: List<ExamScope.CustomRange> = emptyList(),

    // Builder State
    val currentTab: SetupTab = SetupTab.SURAH,
    val selectedSurah: Int = 1,
    val selectedJuz: Int = 1,
    val selectedRub: Int = 1,
    val selectedAyahRange: Pair<Pair<Int, Int>, Pair<Int, Int>> = Pair(1 to 1, 1 to 7),
    val customScopeName: String = "",
    val customScopeComponents: List<ExamScope> = emptyList(),
) {
    val canStart: Boolean get() = if (isCreatingNewRange) {
        !questionCountRange.isEmpty() && !isInvalidRange
    } else {
        activeScope != null && !questionCountRange.isEmpty() && !isInvalidRange
    }

    val isInvalidRange: Boolean get() {
        if (currentTab == SetupTab.AYAH && isCreatingNewRange) {
            val startSurah = selectedAyahRange.first.first
            val startAyah = selectedAyahRange.first.second
            val endSurah = selectedAyahRange.second.first
            val endAyah = selectedAyahRange.second.second
            if (startSurah > endSurah) return true
            if (startSurah == endSurah && startAyah > endAyah) return true
        }
        return false
    }
}

enum class SetupTab {
    SURAH, JUZ, RUB, AYAH
}