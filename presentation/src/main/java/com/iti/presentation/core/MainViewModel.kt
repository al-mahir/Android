package com.iti.presentation.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.connectivity.ConnectivityObserver
import com.iti.domain.connectivity.ConnectivityStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

enum class ConnectivityBanner { Hidden, Offline, BackOnline }

class MainViewModel(
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = connectivityObserver.status
        .map { it == ConnectivityStatus.Available }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val banner: StateFlow<ConnectivityBanner> = flow {
        var wasOffline = false
        connectivityObserver.status.collect { status ->
            when (status) {
                ConnectivityStatus.Unavailable -> { 
                    wasOffline = true; emit(ConnectivityBanner.Offline) 
                }
                ConnectivityStatus.Available -> {
                    if (wasOffline) {
                        emit(ConnectivityBanner.BackOnline)
                        delay(3000)
                        emit(ConnectivityBanner.Hidden)
                        wasOffline = false
                    } else {
                        emit(ConnectivityBanner.Hidden)
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectivityBanner.Hidden)

    val reconnectSignal: SharedFlow<Unit> = connectivityObserver.status
        .map { it == ConnectivityStatus.Available }
        .distinctUntilChanged()
        .drop(1)
        .filter { it }
        .map { }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))
}
