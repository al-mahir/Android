package com.iti.presentation.profile.navigation

import androidx.navigation3.runtime.NavKey
import com.iti.domain.model.LegalDocumentType
import kotlinx.serialization.Serializable


sealed interface ProfileRoute : NavKey {

    @Serializable
    data class StaticContent(val documentType: LegalDocumentType) : ProfileRoute

    @Serializable
    data object Premium : ProfileRoute
}
