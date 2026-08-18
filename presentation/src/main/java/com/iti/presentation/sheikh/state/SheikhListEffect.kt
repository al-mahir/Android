package com.iti.presentation.sheikh.state

import androidx.annotation.StringRes

sealed interface SheikhListEffect {
    data class NavigateToSheikhDetails(val sheikhId: String) : SheikhListEffect
    data object NavigateBack : SheikhListEffect
    data class ShowMessage(@StringRes val messageRes: Int) : SheikhListEffect
}
