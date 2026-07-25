package com.example.mushaf.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        onNavigateBack = onNavigateBack,
        onIntent = viewModel::onIntent
    )
}

@Composable
internal fun MushafSearchContent(
    state: MushafSearchState,
    onNavigateBack: () -> Unit,
    onIntent: (MushafSearchIntent) -> Unit
) {
    Scaffold(
        containerColor = Theme.colors.backGround,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .padding(horizontal = 18.dp)
        ) {
            TopHeaderSection(userInitials = "ق", onBack = onNavigateBack) // As requested by user: 'ق'
            
            SearchBar(
                query = state.query,
                onQueryChange = { onIntent(MushafSearchIntent.UpdateQuery(it)) },
                hint = stringResource(R.string.search_hint)
            )
            
            Spacer(Modifier.height(10.dp))

            // Search Type Switcher (Text vs Meaning)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Theme.colors.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (state.searchType == SearchType.TEXT) Theme.colors.primary else Color.Transparent
                        )
                        .clickable { onIntent(MushafSearchIntent.SelectSearchType(SearchType.TEXT)) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "النص (Wording)",
                        color = if (state.searchType == SearchType.TEXT) Color.White else Theme.colors.primaryFont,
                        style = Theme.typography.body.medium
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (state.searchType == SearchType.MEANING) Theme.colors.primary else Color.Transparent
                        )
                        .clickable { onIntent(MushafSearchIntent.SelectSearchType(SearchType.MEANING)) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "المعنى (Meaning)",
                        color = if (state.searchType == SearchType.MEANING) Color.White else Theme.colors.primaryFont,
                        style = Theme.typography.body.medium
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (state.searchType == SearchType.TAFSIR) Theme.colors.primary else Color.Transparent
                        )
                        .clickable { onIntent(MushafSearchIntent.SelectSearchType(SearchType.TAFSIR)) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "التفسير (Tafsir)",
                        color = if (state.searchType == SearchType.TAFSIR) Color.White else Theme.colors.primaryFont,
                        style = Theme.typography.body.medium
                    )
                }
            }

            if (state.searchType == SearchType.MEANING && state.hydeUsed) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "✨ تم توسيع البحث بالذكاء الاصطناعي (HyDE Expansion)",
                    style = Theme.typography.body.small,
                    color = Theme.colors.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (state.errorMessage != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.errorMessage,
                    style = Theme.typography.body.small,
                    color = Color.Red,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            val listState = rememberLazyListState()

            LaunchedEffect(listState) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .collect { lastIndex ->
                        if (state.query.isNotBlank() && lastIndex != null) {
                            if (state.searchType == SearchType.TEXT && lastIndex >= state.surahs.size + state.ayahs.size - 5) {
                                onIntent(MushafSearchIntent.LoadNextAyahsPage)
                            } else if (state.searchType == SearchType.TAFSIR && lastIndex >= state.tafsirs.size - 5) {
                                onIntent(MushafSearchIntent.LoadNextAyahsPage)
                            }
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
                    if (state.searchType == SearchType.TEXT) {
                        items(state.surahs, key = { it.number }) { surah ->
                            SurahListItem(
                                surah = surah,
                                query = state.query,
                                onClick = { onIntent(MushafSearchIntent.SurahClicked(it)) }
                            )
                        }
                    }
                    if (state.searchType == SearchType.TAFSIR) {
                        items(state.tafsirs, key = { "tafsir-${it.surahNumber}-${it.ayahNumber}" }) { tafsir ->
                            com.example.mushaf.presentation.search.components.TafsirListItem(
                                tafsir = tafsir,
                                query = state.query,
                                onClick = { onIntent(MushafSearchIntent.TafsirClicked(tafsir)) }
                            )
                        }
                    } else {
                        items(state.ayahs, key = { "${it.surahNumber}-${it.ayahNumber}" }) { ayah ->
                            AyahListItem(
                                ayah = ayah,
                                query = state.query,
                                onClick = { onIntent(MushafSearchIntent.AyahClicked(it)) }
                            )
                        }
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
