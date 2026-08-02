package com.iti.sheikh.presentation.home.state

import androidx.annotation.StringRes

sealed interface SheikhHomeEffect {
    data object OpenProfile : SheikhHomeEffect
    data class OpenActiveCall(
        val requestId: String,
        val token: String,
        val channelName: String,
        val userAccount: String,
        val remoteDisplayName: String?,
    ) : SheikhHomeEffect
    data class ShowMessage(@StringRes val messageRes: Int) : SheikhHomeEffect
}
