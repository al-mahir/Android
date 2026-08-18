package com.iti.presentation.meetingrequest.request


sealed interface RequestEffect {
    data class ShowMessage(val message: String) : RequestEffect

    /** Sends a blocked student to the packages list so they can buy or renew. */
    data object OpenPackages : RequestEffect
}
