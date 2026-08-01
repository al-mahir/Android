package com.iti.presentation.meetingrequest.request


sealed interface RequestEffect {
    data class ShowMessage(val message: String) : RequestEffect
}
