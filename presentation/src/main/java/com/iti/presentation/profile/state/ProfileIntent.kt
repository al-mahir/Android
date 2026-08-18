package com.iti.presentation.profile.state

import com.iti.presentation.profile.model.ProfileMenuType
import com.iti.presentation.profile.model.SocialChannel

sealed interface ProfileIntent {

    data object Retry : ProfileIntent

    /** Re-reads the subscription entitlement — fired whenever the screen resumes, so a
     * package bought on the checkout screen is reflected on return instead of after a
     * cold start. */
    data object Refresh : ProfileIntent

    data object PremiumClicked : ProfileIntent

    data object MySubscriptionClicked : ProfileIntent

    data object LogoutClicked : ProfileIntent

    data object DeleteAccountClicked : ProfileIntent

    data object DialogConfirmed : ProfileIntent

    data object DialogDismissed : ProfileIntent

    data class MenuOptionClicked(val menuType: ProfileMenuType) : ProfileIntent

    data class SocialChannelClicked(val channel: SocialChannel) : ProfileIntent

    data object SeeAllCirclesClicked : ProfileIntent

    data class CircleClicked(val circleId: String) : ProfileIntent
}
