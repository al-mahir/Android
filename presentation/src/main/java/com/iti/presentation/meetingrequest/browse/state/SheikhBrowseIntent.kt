package com.iti.presentation.meetingrequest.browse

sealed interface SheikhBrowseIntent {
    data object Load : SheikhBrowseIntent
    data object Refresh : SheikhBrowseIntent
    data class SheikhSelected(val sheikhId: String) : SheikhBrowseIntent
}


