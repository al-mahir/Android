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

    @Serializable
    data class SurahDownload(val reciterId: Int) : DownloadsRoute
}

fun EntryProviderScope<NavKey>.downloadsEntries(
    onBack: () -> Unit,
    onNavigateToSurahList: (Int) -> Unit = {},
) {
    entry<DownloadsRoute.Downloads> { route ->
        DownloadsScreen(
            kind = route.kind,
            onBack = onBack,
            onNavigateToSurahList = { rawId ->
                val reciterId = rawId.toIntOrNull()
                    ?: when (rawId.removePrefix("reciter_").lowercase()) {
                        "husary" -> 6
                        "minshawi" -> 9
                        "abdulbasit" -> 1
                        "sudais" -> 3
                        "shatri" -> 4
                        "afasy", "alafasy" -> 7
                        "rifai" -> 5
                        "tablawi" -> 11
                        "shuraym" -> 10
                        else -> 7
                    }
                onNavigateToSurahList(reciterId)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
    
    entry<DownloadsRoute.SurahDownload> { route ->
        com.example.mushaf.presentation.download.SurahDownloadScreen(
            reciterId = route.reciterId,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
