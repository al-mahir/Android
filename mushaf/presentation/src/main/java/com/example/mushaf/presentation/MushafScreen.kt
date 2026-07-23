package com.example.mushaf.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.mushaf.AudioPlayerBar
import com.example.designsystem.components.mushaf.ReciterItem
import com.example.designsystem.components.mushaf.ReciterPickerSheet
import com.example.designsystem.components.mushaf.SurahItem
import com.example.designsystem.components.mushaf.SurahPickerSheet
import com.example.designsystem.components.mushaf.TajweedLegendSheet
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.domain.model.SurahCatalog
import com.example.mushaf.domain.model.SurahOrigin
import com.example.mushaf.presentation.audio.AudioState
import com.example.mushaf.presentation.components.MushafBottomBar
import com.example.mushaf.presentation.components.MushafErrorState
import com.example.mushaf.presentation.components.MushafLoading
import com.example.mushaf.presentation.components.MushafPageView
import com.example.mushaf.presentation.components.MushafTopBar
import com.example.mushaf.presentation.state.MushafIntent
import com.example.mushaf.presentation.state.PageLoadState
import org.koin.androidx.compose.koinViewModel

@Composable
fun MushafScreen(
    modifier: Modifier = Modifier,
    /**
     * Page to open on, e.g. when arriving from Home's "Continue Reading". Null resumes the
     * reader's own persisted last page.
     */
    startPage: Int? = null,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    viewModel: MushafViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = state.currentPage - 1,
        pageCount = { state.pageCount },
    )
    
    var showReciterPicker by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onIntent(MushafIntent.LoadPage(pagerState.currentPage + 1))
    }

    // Declared after the pager effect on purpose. `state.currentPage` is the single source of
    // truth for which page is shown; seeding the pager instead would make the two fight and
    // oscillate. Dispatching here sets the state, and the sync effect below scrolls the pager
    // to match. OpenAtPage (not LoadPage) so a late preferences emission cannot restore the
    // previously-read page over the one the caller asked for.
    LaunchedEffect(startPage) {
        if (startPage != null) {
            viewModel.onIntent(MushafIntent.OpenAtPage(startPage))
        }
    }

    LaunchedEffect(state.currentPage) {
        val target = (state.currentPage - 1).coerceIn(0, state.pageCount - 1)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 2,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            viewModel.onIntent(MushafIntent.ToggleBars)
                        }
                    },
            ) { pageIndex ->
                val pageNumber = pageIndex + 1
                Box(modifier = Modifier.fillMaxSize().graphicsLayer()) {
                    when (val pageState = state.pageState(pageNumber)) {
                        is PageLoadState.Loaded ->
                            MushafPageView(
                                page = pageState.page,
                                mode = state.readingMode,
                                highlightedWordId = if (pageNumber == state.currentPage) state.highlightedWordId else null,
                                prefetchPages = if (pageNumber == state.currentPage) {
                                    listOfNotNull(
                                        state.pages[pageNumber - 2],
                                        state.pages[pageNumber - 1],
                                        state.pages[pageNumber + 1],
                                        state.pages[pageNumber + 2],
                                    )
                                } else {
                                    emptyList()
                                },
                                areAyahsHidden = !state.areAyahsVisible,
                                revealedWordIds = state.revealedWordIds,
                            )

                        PageLoadState.Failed ->
                            MushafErrorState(onRetry = { viewModel.onIntent(MushafIntent.Retry) })

                        PageLoadState.Loading -> MushafLoading()
                    }
                }
            }
        }

        MushafTopBar(
            visible = state.areBarsVisible,
            surahName = SurahNameResolver.nameFor(state.currentSurahNumber),
            juzNumber = 1,
            hizbNumber = 1,
            isBookmarked = false,
            onBack = onBack,
            onBookmark = {},
            onSettings = onOpenSettings,
            onSurahNameClick = { viewModel.onIntent(MushafIntent.ShowSurahPicker) },
            onSearchClick = onSearchClick,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (state.mushafMode == MushafMode.LISTEN && state.areBarsVisible) {
            AudioPlayerBar(
                isPlaying = state.audioState == AudioState.PLAYING,
                reciterName = state.currentReciter?.nameArabic ?: "",
                playbackSpeed = state.playbackSpeed,
                onPlayPauseClick = { viewModel.onIntent(MushafIntent.PlayPauseAudio) },
                onNextClick = { viewModel.onIntent(MushafIntent.NextAyahAudio) },
                onPrevClick = { viewModel.onIntent(MushafIntent.PrevAyahAudio) },
                onReciterClick = { showReciterPicker = true },
                onSpeedClick = {
                    val nextSpeed = when (state.playbackSpeed) {
                        0.75f -> 1.0f
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        else -> 0.75f
                    }
                    viewModel.onIntent(MushafIntent.SetAudioSpeed(nextSpeed))
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            MushafBottomBar(
                visible = state.areBarsVisible,
                mushafMode = state.mushafMode,
                areAyahsVisible = state.areAyahsVisible,
                isRecordingActive = state.isRecordingActive,
                onToggleAyahVisibility = { viewModel.onIntent(MushafIntent.ToggleAyahVisibility) },
                onRevealNextWord = { viewModel.onIntent(MushafIntent.RevealNextWord) },
                onRevealNextAyah = { viewModel.onIntent(MushafIntent.RevealNextAyah) },
                onModeSelected = { mode -> viewModel.onIntent(MushafIntent.SetMode(mode)) },
                onToggleRecording = { viewModel.onIntent(MushafIntent.ToggleRecording) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // ── Tajweed Legend FAB ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.areBarsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 80.dp), // above the bottom bar
        ) {
            FloatingActionButton(
                onClick = { viewModel.onIntent(MushafIntent.ShowTajweedLegend) },
                shape = CircleShape,
                containerColor = Theme.colors.surface,
                contentColor = Theme.colors.primary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = "دليل ألوان التجويد",
                )
            }
        }
        
        // ── Reciter Picker sheet ────────────────────────────────────────────────
        if (showReciterPicker) {
            val reciterItems = state.availableReciters.map {
                ReciterItem(
                    id = it.id,
                    name = it.name,
                    nameArabic = it.nameArabic,
                    style = it.style.name.lowercase().replaceFirstChar { char -> char.uppercase() }
                )
            }
            ReciterPickerSheet(
                reciters = reciterItems,
                selectedId = state.currentReciter?.id,
                onReciterSelected = { selectedItem ->
                    val domainReciter = state.availableReciters.first { it.id == selectedItem.id }
                    viewModel.onIntent(MushafIntent.SelectReciter(domainReciter))
                    showReciterPicker = false
                },
                onDismiss = { showReciterPicker = false }
            )
        }

        // ── Surah Picker sheet ──────────────────────────────────────────────────
        if (state.showSurahPicker) {
            val surahItems = remember {
                SurahCatalog.all.map { s ->
                    SurahItem(
                        number = s.number,
                        nameArabic = s.nameArabic,
                        nameEnglish = s.nameEnglish,
                        isMeccan = s.origin == com.example.mushaf.domain.model.SurahOrigin.MECCAN,
                    )
                }
            }
            SurahPickerSheet(
                surahs = surahItems,
                currentSurahNumber = state.currentSurahNumber,
                onSurahSelected = { item ->
                    viewModel.onIntent(MushafIntent.NavigateToSurah(item.number))
                },
                onDismiss = { viewModel.onIntent(MushafIntent.HideSurahPicker) },
            )
        }

        // ── Tajweed Legend sheet ────────────────────────────────────────────────
        if (state.showTajweedLegend) {
            TajweedLegendSheet(
                onDismiss = { viewModel.onIntent(MushafIntent.HideTajweedLegend) },
            )
        }
    }
}
