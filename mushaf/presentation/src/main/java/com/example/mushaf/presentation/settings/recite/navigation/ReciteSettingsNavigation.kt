package com.example.mushaf.presentation.settings.recite.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.mushaf.presentation.settings.recite.ReciteSettingsScreen
import kotlinx.serialization.Serializable

sealed interface ReciteSettingsRoute : NavKey {

    @Serializable
    data object ReciteSettings : ReciteSettingsRoute
}

fun EntryProviderScope<NavKey>.reciteSettingsEntries(
    onBack: () -> Unit,
) {
    entry<ReciteSettingsRoute.ReciteSettings> {
        ReciteSettingsScreen(
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
