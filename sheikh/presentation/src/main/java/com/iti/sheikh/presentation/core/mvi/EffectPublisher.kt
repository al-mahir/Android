package com.iti.sheikh.presentation.core.mvi

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * One-off effect stream for an MVI screen (navigation, toasts, snackbars) — the part of the
 * contract that must NOT live in state, because it fires once and is not replayed on
 * configuration change.
 *
 * ViewModels obtain this by delegation rather than inheritance:
 * `class FooViewModel : ViewModel(), EffectPublisher<FooEffect> by DefaultEffectPublisher()`.
 */
interface EffectPublisher<E> {
    val effect: Flow<E>

    fun sendEffect(effect: E)
}

/**
 * Channel-backed [EffectPublisher]. `Channel.BUFFERED` + `receiveAsFlow()` gives a
 * single-consumer, no-replay stream: effects emitted while the UI is stopped are buffered
 * and delivered once collection resumes, and each effect is handled exactly once.
 */
class DefaultEffectPublisher<E> : EffectPublisher<E> {
    private val _effect = Channel<E>(Channel.BUFFERED)
    override val effect: Flow<E> = _effect.receiveAsFlow()

    override fun sendEffect(effect: E) {
        _effect.trySend(effect)
    }
}
