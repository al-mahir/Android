package com.iti.presentation.core.mvi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single immutable state holder for an MVI screen. Exposes a read-only [StateFlow] to the UI
 * while keeping mutation behind [updateState], so reducers are the only way state changes.
 *
 * ViewModels obtain this by delegation rather than inheritance:
 * `class FooViewModel : ViewModel(), StateHolder<FooUiState> by DefaultStateHolder(FooUiState())`.
 */
interface StateHolder<S> {
    val state: StateFlow<S>

    val currentState: S get() = state.value

    fun updateState(transform: S.() -> S)
}

/**
 * [MutableStateFlow]-backed [StateHolder]. [updateState] uses `update`, which is atomic and
 * safe to call from concurrent coroutines.
 */
class DefaultStateHolder<S>(initialState: S) : StateHolder<S> {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<S> = _state.asStateFlow()

    override fun updateState(transform: S.() -> S) {
        _state.update(transform)
    }
}
