package com.iti.presentation.profile.state

import androidx.annotation.StringRes
import com.iti.domain.model.LegalDocumentType
import com.iti.presentation.profile.model.ProfileWebTarget
import com.iti.presentation.profile.model.SocialChannel

sealed interface ProfileEffect {

    data object OpenPremium : ProfileEffect

    data object OpenMySubscription : ProfileEffect

    data object OpenSettings : ProfileEffect

    /** The reciter wants their past sessions. */
    data object OpenSessions : ProfileEffect

    /** Content credits. Required by the Qur'an text licence, so it is always reachable. */
    data object OpenAttributions : ProfileEffect

    data class OpenLegalDocument(val type: LegalDocumentType) : ProfileEffect

    data class OpenWebPage(val target: ProfileWebTarget) : ProfileEffect

    data class OpenSocialChannel(val channel: SocialChannel) : ProfileEffect

    data object ShareApp : ProfileEffect

    data object RequestAppReview : ProfileEffect

    data object NavigateToAuth : ProfileEffect

    data class ShowMessage(@StringRes val messageRes: Int) : ProfileEffect
}
