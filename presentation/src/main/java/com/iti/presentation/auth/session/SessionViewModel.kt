package com.iti.presentation.auth.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.auth.usecase.ObserveAuthStateUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


sealed interface SessionState {
    data object Resolving : SessionState
    data object Authenticated : SessionState
    data object SignedOut : SessionState
}

class SessionViewModel(
    observeAuthState: ObserveAuthStateUseCase,
) : ViewModel() {

    val state: StateFlow<SessionState> = observeAuthState()
        .map { isAuthenticated ->
            if (isAuthenticated) SessionState.Authenticated else SessionState.SignedOut
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SessionState.Resolving,
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
