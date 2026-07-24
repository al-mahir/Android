package com.iti.presentation.settings.navigation

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.iti.presentation.settings.SettingsScreen

fun EntryProviderScope<NavKey>.settingsEntries(
    onBack: () -> Unit,
) {
    entry<SettingsRoute.Settings> {
        val context = LocalContext.current

        SettingsScreen(
            onBack = onBack,
            onShowMessage = { messageRes ->
                Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
            }
        )
    }
}
