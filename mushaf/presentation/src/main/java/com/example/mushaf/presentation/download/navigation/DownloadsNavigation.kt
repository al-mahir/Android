package com.example.mushaf.presentation.download.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.example.mushaf.domain.model.ResourceKind
import com.example.mushaf.presentation.download.DownloadsScreen
import kotlinx.serialization.Serializable


sealed interface DownloadsRoute : NavKey {

    @Serializable
    data class Downloads(val kind: ResourceKind) : DownloadsRoute
}

fun EntryProviderScope<NavKey>.downloadsEntries(
    onBack: () -> Unit,
) {
    entry<DownloadsRoute.Downloads> { route ->
        DownloadsScreen(
            kind = route.kind,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
