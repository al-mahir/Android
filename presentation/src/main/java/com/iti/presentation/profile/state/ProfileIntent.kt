package com.iti.presentation.profile.state

import com.iti.presentation.profile.model.ProfileMenuType
import com.iti.presentation.profile.model.SocialChannel

sealed interface ProfileIntent {

    data object Retry : ProfileIntent

    data object PremiumClicked : ProfileIntent

    data object RestorePurchasesClicked : ProfileIntent

    data object LogoutClicked : ProfileIntent

    data object DeleteAccountClicked : ProfileIntent

    data object DialogConfirmed : ProfileIntent

    data object DialogDismissed : ProfileIntent

    data class MenuOptionClicked(val menuType: ProfileMenuType) : ProfileIntent

    data class SocialChannelClicked(val channel: SocialChannel) : ProfileIntent
}
