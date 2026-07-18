package com.iti.presentation.core.mvi

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

interface UiState
interface UiIntent
interface UiEffect

abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiEffect> : ViewModel() {

    private val initialState: S by lazy { createInitialState() }

    private val _uiState: MutableStateFlow<S> = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _uiEffect: MutableSharedFlow<E> = MutableSharedFlow()
    val uiEffect: SharedFlow<E> = _uiEffect.asSharedFlow()

    abstract fun createInitialState(): S

    abstract fun handleIntent(intent: I)

    fun sendIntent(intent: I) {
        handleIntent(intent)
    }

    protected fun setState(reduce: S.() -> S) {
        val newState = currentState.reduce()
        _uiState.value = newState
    }

    protected fun setEffect(builder: () -> E) {
        val effectValue = builder()
        viewModelScope.launch { _uiEffect.emit(effectValue) }
    }

    protected val currentState: S
        get() = uiState.value
}
