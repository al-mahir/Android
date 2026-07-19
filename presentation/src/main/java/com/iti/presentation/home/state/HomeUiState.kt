package com.iti.presentation.home.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.iti.domain.model.ReadingProgress
import com.iti.domain.model.Sheikh
import com.iti.domain.model.StudyCircle
import com.iti.domain.model.User


@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
    val user: User? = null,
    val readingProgress: ReadingProgress? = null,
    val sheikhs: List<Sheikh> = emptyList(),
    val circles: List<StudyCircle> = emptyList(),
    val joiningCircleIds: Set<String> = emptySet(),
) {
    val hasError: Boolean get() = errorMessageRes != null
}
