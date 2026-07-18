package com.iti.presentation.home.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.iti.domain.model.home.ContinueReading
import com.iti.domain.model.home.Sheikh
import com.iti.domain.model.home.StudyCircle
import com.iti.domain.model.home.UserSummary

/**
 * The complete, immutable rendering contract for the Home screen. `HomeContent` is a pure
 * function of this — nothing else.
 *
 * Marked `@Immutable` so Compose treats the `List` fields as stable and skips recomposition
 * when the instance is unchanged.
 */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    /** Non-null renders the error state instead of the content. */
    @StringRes val errorMessageRes: Int? = null,
    val user: UserSummary? = null,
    val continueReading: ContinueReading? = null,
    val sheikhs: List<Sheikh> = emptyList(),
    val circles: List<StudyCircle> = emptyList(),
    /** Circles with a join request in flight — drives the per-row button spinner. */
    val joiningCircleIds: Set<String> = emptySet(),
) {
    val hasError: Boolean get() = errorMessageRes != null
}
