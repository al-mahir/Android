package com.iti.presentation.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.getOrNull
import com.iti.domain.model.recitation.RecitationSessionSummary
import com.iti.domain.usecase.DeleteRecitationSessionUseCase
import com.iti.domain.usecase.ObserveRecitationSessionsUseCase
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.StateHolder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SessionHistoryUiState(
    val isLoading: Boolean = true,
    val sessions: List<RecitationSessionSummary> = emptyList(),
    /** The session opened for detail, if any. */
    val selectedId: String? = null,
) {
    val selected: RecitationSessionSummary?
        get() = selectedId?.let { id -> sessions.firstOrNull { it.id == id } }

    val isEmpty: Boolean get() = !isLoading && sessions.isEmpty()
}

sealed interface SessionHistoryIntent {
    data class Select(val id: String?) : SessionHistoryIntent
    data class Delete(val id: String) : SessionHistoryIntent
}

/**
 * The reciter's past sessions.
 *
 * Reads straight from local storage: history is written by the muṣḥaf and read here, with no
 * network in between. When a backend arrives the repository gains a sync and this does not change.
 */
class SessionHistoryViewModel(
    private val observeSessions: ObserveRecitationSessionsUseCase,
    private val deleteSession: DeleteRecitationSessionUseCase,
) : ViewModel(),
    StateHolder<SessionHistoryUiState> by DefaultStateHolder(SessionHistoryUiState()) {

    init {
        observeSessions()
            .catch { updateState { copy(isLoading = false) } }
            .onEach { result ->
                val sessions = result.getOrNull() ?: return@onEach
                updateState { copy(isLoading = false, sessions = sessions) }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: SessionHistoryIntent) {
        when (intent) {
            is SessionHistoryIntent.Select -> updateState { copy(selectedId = intent.id) }
            is SessionHistoryIntent.Delete -> {
                // Close the detail first: leaving it open on a row that is about to vanish would
                // show a stale session for a frame.
                updateState { copy(selectedId = selectedId.takeIf { it != intent.id }) }
                viewModelScope.launch { deleteSession(intent.id) }
            }
        }
    }
}
