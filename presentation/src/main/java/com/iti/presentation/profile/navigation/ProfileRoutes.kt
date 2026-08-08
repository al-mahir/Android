package com.iti.presentation.profile.navigation

import androidx.navigation3.runtime.NavKey
import com.iti.domain.model.LegalDocumentType
import kotlinx.serialization.Serializable


sealed interface ProfileRoute : NavKey {

    @Serializable
    data class StaticContent(val documentType: LegalDocumentType) : ProfileRoute

    @Serializable
    data object Premium : ProfileRoute

    @Serializable
    data object MySubscription : ProfileRoute

    @Serializable
    data class Checkout(val packageId: String) : ProfileRoute

    /** The reciter's recorded sessions. */
    @Serializable
    data object Sessions : ProfileRoute

    /** Content credits, required by the Qur'an text licence. */
    @Serializable
    data object Attributions : ProfileRoute
}
