package com.iti.presentation.meetingrequest.request

sealed interface RequestIntent {
    data class Send(val sheikhId: String, val note: String?) : RequestIntent
    data object Cancel : RequestIntent
}


