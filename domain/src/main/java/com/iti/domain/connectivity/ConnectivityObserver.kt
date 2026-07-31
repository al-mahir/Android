package com.iti.domain.connectivity

import kotlinx.coroutines.flow.Flow

enum class ConnectivityStatus { Available, Unavailable }

interface ConnectivityObserver {
    /** Emits current status immediately, then on every change. Never completes. */
    val status: Flow<ConnectivityStatus>

    /** Best-effort synchronous read for pre-flight guards (e.g., before a login call). */
    fun currentStatus(): ConnectivityStatus
}
