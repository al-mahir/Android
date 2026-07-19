package com.example.mushaf.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.search.SearchBar
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.MushafFilter
import com.example.mushaf.domain.model.Surah
import com.example.mushaf.presentation.search.components.AyahListItem
import com.example.mushaf.presentation.search.components.FilterTabsRow
import com.example.mushaf.presentation.search.components.HizbListItem
import com.example.mushaf.presentation.search.components.JuzListItem
import com.example.mushaf.presentation.search.components.LastReadBanner
import com.example.mushaf.presentation.search.components.MushafBottomBar
import com.example.mushaf.presentation.search.components.PageListItem
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
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
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
                .padding(padding)
                .padding(horizontal = 18.dp)
        ) {
            TopHeaderSection(userInitials = "ق") // As requested by user: 'ق'
            Spacer(Modifier.height(8.dp))
            
            SearchBar(
                query = state.query,
                onQueryChange = { onIntent(MushafSearchIntent.UpdateQuery(it)) },
                hint = "Search Surah, Para, Page..."
            )
            
            Spacer(Modifier.height(14.dp))
            
            FilterTabsRow(
                selected = state.selectedFilter,
                onSelect = { onIntent(MushafSearchIntent.SelectFilter(it)) }
            )
            
            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                when (state.selectedFilter) {
                    MushafFilter.SURAH -> {
                        items(state.surahs, key = { it.number }) { surah ->
                            SurahListItem(
                                surah = surah,
                                onClick = { onIntent(MushafSearchIntent.SurahClicked(it)) }
                            )
                        }
                    }
                    MushafFilter.PARA -> {
                        items(state.juzs, key = { it.number }) { juz ->
                            JuzListItem(
                                juz = juz,
                                onClick = { onIntent(MushafSearchIntent.JuzClicked(it)) }
                            )
                        }
                    }
                    MushafFilter.PAGE -> {
                        items(state.pages, key = { it }) { page ->
                            PageListItem(
                                page = page,
                                onClick = { onIntent(MushafSearchIntent.PageClicked(it)) }
                            )
                        }
                    }
                    MushafFilter.HIJB -> {
                        items(state.hizbs, key = { it.number }) { hizb ->
                            HizbListItem(
                                hizb = hizb,
                                onClick = { onIntent(MushafSearchIntent.HizbClicked(it)) }
                            )
                        }
                    }
                    MushafFilter.AYAH -> {
                        items(state.ayahs, key = { "${it.surahNumber}-${it.ayahNumber}" }) { ayah ->
                            AyahListItem(
                                ayah = ayah,
                                onClick = { onIntent(MushafSearchIntent.AyahClicked(it)) }
                            )
                        }
                    }
                }
            }

            state.lastReadSession?.let { lastRead ->
                LastReadBanner(
                    surahName = lastRead.surahName,
                    ayah = lastRead.ayah,
                    page = lastRead.page,
                    onClick = { onIntent(MushafSearchIntent.LastReadClicked) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}
