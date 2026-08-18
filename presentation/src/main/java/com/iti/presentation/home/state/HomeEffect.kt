package com.iti.presentation.home.state

import androidx.annotation.StringRes


sealed interface HomeEffect {
    data object OpenSearch : HomeEffect
    data object OpenProfile : HomeEffect
    data object OpenSheikhList : HomeEffect
    data object OpenCircleList : HomeEffect
    data class OpenMushafAtPage(val page: Int) : HomeEffect
    data class OpenSheikh(val sheikhId: String) : HomeEffect
    data class OpenCircle(val circleId: String) : HomeEffect
    data class OpenMeetingRequest(val sheikhId: String, val sheikhName: String?) : HomeEffect
    data class OpenActiveCall(
        val requestId: String,
        val token: String,
        val channelName: String,
        val userAccount: String,
        val remoteDisplayName: String?,
    ) : HomeEffect
    data class ShowMessage(@StringRes val messageRes: Int) : HomeEffect
    data object OpenExamSetup : HomeEffect
}
