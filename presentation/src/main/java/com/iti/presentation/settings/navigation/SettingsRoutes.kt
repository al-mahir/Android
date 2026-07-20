package com.iti.presentation.settings.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface SettingsRoute : NavKey {

    @Serializable
    data object Settings : SettingsRoute
}
