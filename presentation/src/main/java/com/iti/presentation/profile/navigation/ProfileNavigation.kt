package com.iti.presentation.profile.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.designsystem.components.placeholderscreens.EmptyDataScreen
import com.iti.presentation.attributions.AttributionsScreen
import com.iti.presentation.sessions.SessionHistoryScreen
import com.iti.presentation.staticcontent.StaticContentScreen


fun EntryProviderScope<NavKey>.profileEntries(
    onBack: () -> Unit,
) {
    entry<ProfileRoute.StaticContent> { route ->
        StaticContentScreen(
            documentType = route.documentType,
            onBack = onBack,
        )
    }

    entry<ProfileRoute.Sessions> {
        SessionHistoryScreen(onBack = onBack)
    }

    entry<ProfileRoute.Attributions> {
        AttributionsScreen(onBack = onBack)
    }

    entry<ProfileRoute.Premium> {
        EmptyDataScreen(modifier = Modifier.fillMaxSize())
    }
}
