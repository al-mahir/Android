package com.iti.presentation.meetingrequest.browse

sealed interface SheikhBrowseEffect {
    data class NavigateToRequest(val sheikhId: String) : SheikhBrowseEffect
}


