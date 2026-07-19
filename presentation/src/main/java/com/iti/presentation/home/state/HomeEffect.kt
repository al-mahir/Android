package com.iti.presentation.home.state

import androidx.annotation.StringRes

/**
 * One-off events — navigation and messages. Kept out of [HomeUiState] so they fire exactly
 * once and are not replayed after a configuration change.
 */
sealed interface HomeEffect {
    data object OpenSearch : HomeEffect
    data object OpenProfile : HomeEffect
    data object OpenSheikhList : HomeEffect
    data object OpenCircleList : HomeEffect
    data class OpenMushafAtPage(val page: Int) : HomeEffect
    data class OpenSheikh(val sheikhId: String) : HomeEffect
    data class ShowMessage(@StringRes val messageRes: Int) : HomeEffect
}
