package com.example.mushaf.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.search.SearchBar
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.R
import com.example.mushaf.domain.model.Surah
import com.example.mushaf.presentation.search.components.AyahListItem
import com.example.mushaf.presentation.search.components.JuzListItem
import com.example.mushaf.presentation.search.components.LastReadBanner
import com.example.mushaf.presentation.search.components.MushafBottomBar
import com.example.mushaf.presentation.search.components.SurahListItem
import com.example.mushaf.presentation.search.components.TopHeaderSection
import com.example.mushaf.presentation.search.components.LastReadBanner
import com.example.mushaf.presentation.search.components.MushafBottomBar
import com.example.mushaf.presentation.search.components.MushafDestination
import com.example.mushaf.presentation.search.components.SurahListItem
import com.example.mushaf.presentation.search.components.TopHeaderSection

@Composable
fun MushafSearchScreen(
    viewModel: MushafSearchViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToMushaf: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(state.shouldNavigateToMushaf) {
        if (state.shouldNavigateToMushaf) {
            onNavigateToMushaf()
            viewModel.onIntent(MushafSearchIntent.ClearNavigationEffect)
        }
    }

    MushafSearchContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}

@Composable
internal fun MushafSearchContent(
    state: MushafSearchState,
    onIntent: (MushafSearchIntent) -> Unit
) {
    Scaffold(
        containerColor = Theme.colors.backGround,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = { 
            MushafBottomBar(
                selected = MushafDestination.MUSHAF, // Mock selected tab
                onSelect = { onIntent(MushafSearchIntent.NavigateBottomTab(it.ordinal)) }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding(), top = 12.dp)
                .padding(horizontal = 18.dp)
        ) {
            TopHeaderSection(userInitials = "ق") // As requested by user: 'ق'
            Spacer(Modifier.height(8.dp))
            
            SearchBar(
                query = state.query,
                onQueryChange = { onIntent(MushafSearchIntent.UpdateQuery(it)) },
                hint = stringResource(R.string.search_hint)
            )
            
            Spacer(Modifier.height(14.dp))

            val listState = rememberLazyListState()

            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .collect { lastIndex ->
                        if (state.query.isNotBlank() && lastIndex != null && lastIndex >= state.surahs.size + state.ayahs.size - 5) {
                            onIntent(MushafSearchIntent.LoadNextAyahsPage)
                        }
                    }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                if (state.query.isBlank()) {
                    items(state.juzs, key = { it.number }) { juz ->
                        JuzListItem(
                            juz = juz,
                            onClick = { onIntent(MushafSearchIntent.JuzClicked(it)) }
                        )
                    }
                } else {
                    items(state.surahs, key = { it.number }) { surah ->
                        SurahListItem(
                            surah = surah,
                            onClick = { onIntent(MushafSearchIntent.SurahClicked(it)) }
                        )
                    }
                    items(state.ayahs, key = { "${it.surahNumber}-${it.ayahNumber}" }) { ayah ->
                        AyahListItem(
                            ayah = ayah,
                            onClick = { onIntent(MushafSearchIntent.AyahClicked(it)) }
                        )
                    }
                    if (state.isPaginatingAyahs) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Theme.colors.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            state.lastReadSession?.let { lastRead ->
                LastReadBanner(
                    surahNameAr = lastRead.surahNameAr,
                    surahNameEn = lastRead.surahNameEn,
                    ayah = lastRead.ayah,
                    page = lastRead.page,
                    onClick = { onIntent(MushafSearchIntent.LastReadClicked) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}
