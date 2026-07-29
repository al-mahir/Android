package com.iti.sheikh.presentation.home.state

import androidx.annotation.StringRes

sealed interface SheikhHomeEffect {
    data object OpenProfile : SheikhHomeEffect
    data class ShowMessage(@StringRes val messageRes: Int) : SheikhHomeEffect
}
