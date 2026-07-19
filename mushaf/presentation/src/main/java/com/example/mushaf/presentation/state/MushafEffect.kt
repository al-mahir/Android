package com.example.mushaf.presentation.state

import androidx.annotation.StringRes

sealed interface MushafEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : MushafEffect
    data object NavigateBack : MushafEffect
}
