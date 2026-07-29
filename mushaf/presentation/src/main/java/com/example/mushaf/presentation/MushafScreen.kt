package com.example.mushaf.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.designsystem.components.mushaf.AudioPlayerBar
import com.example.designsystem.components.mushaf.ReciterItem
import com.example.designsystem.components.mushaf.ReciterPickerSheet
import com.example.designsystem.components.mushaf.SurahItem
import com.example.designsystem.components.mushaf.SurahPickerSheet
import com.example.designsystem.components.mushaf.TajweedLegendSheet
import com.example.designsystem.components.mushaf.CorrectionsSheet
import com.example.designsystem.components.session.SessionSummarySheet
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.domain.model.SurahCatalog
import com.example.mushaf.domain.model.SurahOrigin
import com.example.mushaf.presentation.audio.AudioState
import com.example.mushaf.presentation.components.GradingModeToggle
import com.example.mushaf.presentation.components.MushafBottomBar
import com.example.mushaf.presentation.components.MushafErrorState
import com.example.mushaf.presentation.components.MushafLoading
import com.example.mushaf.presentation.components.MushafPageView
import com.example.mushaf.presentation.components.MushafTopBar
import com.example.mushaf.presentation.recite.LiveSessionStatusRow
import com.example.mushaf.presentation.recite.CorrectionFilter
import com.example.mushaf.presentation.recite.CorrectionsUiMapper
import com.example.mushaf.presentation.recite.correctionsSubtitle
import com.example.mushaf.presentation.recite.label
import com.example.mushaf.presentation.recite.toChip
import com.example.mushaf.presentation.recite.breakdown
import com.example.mushaf.presentation.recite.emptyMessageOrNull
import com.example.mushaf.presentation.recite.headline
import com.example.mushaf.presentation.recite.practiceLines
import com.example.mushaf.presentation.recite.stats
import com.example.mushaf.presentation.recite.subtitle
import com.example.mushaf.presentation.recite.toCard
import com.example.mushaf.presentation.recite.MicPrompt
import com.example.mushaf.presentation.recite.MicPromptDialog
import com.example.mushaf.presentation.recite.hasRecordAudioPermission
import com.example.mushaf.presentation.recite.openAppSettings
import com.example.mushaf.presentation.state.CaptureError
import com.example.mushaf.presentation.state.MushafIntent
import com.example.mushaf.presentation.state.PageLoadState
import org.koin.androidx.compose.koinViewModel

@Composable
fun MushafScreen(
    modifier: Modifier = Modifier,
    startPage: Int? = null,
    onBack: () -> Unit = {},
    onNavigateSearch: () -> Unit = {},
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

    val context = LocalContext.current
    var micPrompt by remember { mutableStateOf<MicPrompt?>(null) }
    var showCorrections by remember { mutableStateOf(false) }
    var correctionTabIndex by remember { mutableStateOf(0) }

    
    
    val wordMarks by remember(state.liveCorrection.wordFeedback) {
        derivedStateOf { state.liveCorrection.wordFeedback.mapValues { (_, word) -> word.mark } }
    }
    val corrections by remember(state.liveCorrection.wordFeedback) {
        derivedStateOf { CorrectionsUiMapper.toCorrections(state.liveCorrection.wordFeedback) }
    }
    val correctionTabs by remember(corrections) {
        derivedStateOf { CorrectionFilter.tabsFor(corrections) }
    }
    val selectedTab = correctionTabs.getOrNull(correctionTabIndex) ?: correctionTabs.firstOrNull()
    val visibleCorrections by remember(corrections, selectedTab) {
        derivedStateOf { CorrectionFilter.apply(corrections, selectedTab?.category) }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.onIntent(MushafIntent.ToggleRecording)
        } else {
            micPrompt = MicPrompt.Denied
        }
    }

    LaunchedEffect(state.captureError) {
        when (state.captureError) {
            CaptureError.PERMISSION_DENIED -> micPrompt = MicPrompt.Denied
            CaptureError.MICROPHONE_UNAVAILABLE -> micPrompt = MicPrompt.Unavailable
            
            
            CaptureError.SERVICE_UNREACHABLE -> micPrompt = MicPrompt.ServiceUnreachable
            null -> Unit
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onScreenStopped()
    }

    
    
    val haptics = LocalHapticFeedback.current
    var lastMistakeCount by remember { mutableStateOf(0) }
    LaunchedEffect(state.liveCorrection.mistakeCount) {
        val count = state.liveCorrection.mistakeCount
        if (count > lastMistakeCount) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        lastMistakeCount = count
    }

    // Pager → ViewModel: notify when the user settles on a new page.
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onIntent(MushafIntent.LoadPage(pagerState.currentPage + 1))
    }

    // ViewModel → Pager: programmatic navigation (e.g. recitation auto-advance).
    LaunchedEffect(state.currentPage) {
        val target = (state.currentPage - 1).coerceIn(0, state.pageCount - 1)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    // Deep-link / explicit start page (e.g. tapped a surah from Home).
    LaunchedEffect(startPage) {
        if (startPage != null) {
            viewModel.onIntent(MushafIntent.OpenAtPage(startPage))
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
                modifier = Modifier.fillMaxSize(),
            ) { pageIndex ->
                val pageNumber = pageIndex + 1
                Box(
                    modifier = Modifier.fillMaxSize().graphicsLayer().pointerInput(Unit) {
                        detectTapGestures {
                            // Only handle taps here if the child doesn't consume them (e.g. Loading/Error states)
                            viewModel.onIntent(MushafIntent.ToggleBars)
                        }
                    }
                ) {
                    when (val pageState = state.pageState(pageNumber)) {
                        is PageLoadState.Loaded -> {
                            val isCurrent = pageNumber == state.currentPage

                            val prefetchPages = if (isCurrent) {
                                val before2 = state.pages[pageNumber - 2]
                                val before1 = state.pages[pageNumber - 1]
                                val after1 = state.pages[pageNumber + 1]
                                val after2 = state.pages[pageNumber + 2]
                                remember(before2, before1, after1, after2) {
                                    listOfNotNull(before2, before1, after1, after2)
                                }
                            } else {
                                emptyList()
                            }

                            MushafPageView(
                                page = pageState.page,
                                mode = state.readingMode,
                                highlightedWordId = if (isCurrent) state.highlightedWordId else null,
                                prefetchPages = prefetchPages,
                                areAyahsHidden = !state.areAyahsVisible,
                                revealedWordIds = state.revealedWordIds,
                                onWordClick = {
                                    viewModel.onIntent(MushafIntent.ToggleBars)
                                },
                                onWordLongClick = { wordId ->
                                    val parts = wordId.split(":")
                                    if (parts.size >= 2) {
                                        val surah = parts[0].toIntOrNull()
                                        val ayah = parts[1].toIntOrNull()
                                        if (surah != null && ayah != null) {
                                            viewModel.onIntent(MushafIntent.LoadTafsir(surah, ayah))
                                        }
                                    }
                                },
                                onBlankClick = {
                                    viewModel.onIntent(MushafIntent.ToggleBars)
                                },
                                wordMarks = if (isCurrent) wordMarks else emptyMap(),
                            )
                        }

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

        androidx.compose.foundation.layout.Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = state.mushafMode == MushafMode.LISTEN && state.areBarsVisible,
                enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),
            ) {
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
                    }
                )
            }
            MushafBottomBar(
                visible = state.areBarsVisible,
                mushafMode = state.mushafMode,
                areAyahsVisible = state.areAyahsVisible,
                isRecordingActive = state.isRecordingActive,
                onToggleAyahVisibility = { viewModel.onIntent(MushafIntent.ToggleAyahVisibility) },
                onRevealNextWord = { viewModel.onIntent(MushafIntent.RevealNextWord) },
                onRevealNextAyah = { viewModel.onIntent(MushafIntent.RevealNextAyah) },
                onModeSelected = { mode -> viewModel.onIntent(MushafIntent.SetMode(mode)) },
                micLevel = state.micLevel,
                canFinishSession = state.isRecordingActive && state.liveCorrection.isActive,
                onFinishSession = { viewModel.onIntent(MushafIntent.FinishAndStartNewSession) },
                
                
                statusRow = if (state.mushafMode == MushafMode.RECITATION) {
                    {
                        LiveSessionStatusRow(
                            live = state.liveCorrection,
                            isRecording = state.isRecordingActive,
                            onCorrectionsClick = { showCorrections = true },
                            onDismissEngineNotice = {
                                viewModel.onIntent(MushafIntent.DismissEngineNotice)
                            },
                            onSelectCandidate = { position ->
                                viewModel.onIntent(MushafIntent.SelectCandidate(position))
                            },
                            onDismissCandidates = {
                                viewModel.onIntent(MushafIntent.DismissCandidates)
                            },
                        )
                    }
                } else {
                    null
                },
                gradingToggle = if (state.mushafMode == MushafMode.RECITATION) {
                    {
                        GradingModeToggle(
                            tajweedGradingEnabled = state.isTajweedGradingEnabled,
                            enabled = state.canGradeTajweed,
                            onSelect = { enabled ->
                                viewModel.onIntent(MushafIntent.SetTajweedGrading(enabled))
                            },
                        )
                    }
                } else {
                    null
                },
                onToggleRecording = {
                    when {
                        
                        state.isRecordingActive -> viewModel.onIntent(MushafIntent.ToggleRecording)
                        context.hasRecordAudioPermission() ->
                            viewModel.onIntent(MushafIntent.ToggleRecording)
                        
                        else -> micPrompt = MicPrompt.Preprompt
                    }
                }
            )
        }

        // ── Tajweed Legend FAB ──────────────────────────────────────────────────
        val fabBottomPadding by animateDpAsState(
            targetValue = if (state.mushafMode == MushafMode.LISTEN) 144.dp else 80.dp,
            label = "fabBottomPadding"
        )
        
        AnimatedVisibility(
            visible = state.areBarsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = fabBottomPadding),
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

        if (showCorrections) {
            CorrectionsSheet(
                title = stringResource(R.string.mushaf_corrections_title),
                subtitle = state.liveCorrection.correctionsSubtitle(),
                corrections = visibleCorrections.map { it.toCard() },
                tabs = correctionTabs.map { it.toChip() },
                selectedTabIndex = correctionTabs.indexOf(selectedTab).coerceAtLeast(0),
                onTabSelected = { correctionTabIndex = it },
                emptyMessage = stringResource(R.string.mushaf_corrections_empty),
                practiceTitle = stringResource(R.string.mushaf_practice_focus_title),
                practiceFocus = state.liveCorrection.practiceFocus.map { it.label() },
                onDismiss = { showCorrections = false },
                onMistakeClick = { wordId ->
                    
                    
                    viewModel.onIntent(MushafIntent.HighlightWord(wordId))
                    viewModel.onIntent(MushafIntent.SelectMistake(wordId))
                    showCorrections = false
                },
            )
        }

        state.sessionSummary?.let { summary ->
            SessionSummarySheet(
                headline = summary.headline(),
                subtitle = summary.subtitle(),
                stats = summary.stats(),
                breakdownTitle = stringResource(R.string.session_breakdown_title),
                breakdown = summary.breakdown(),
                practiceTitle = stringResource(R.string.mushaf_practice_focus_title),
                practiceFocus = summary.practiceLines(),
                emptyMessage = summary.emptyMessageOrNull(),
                dismissLabel = stringResource(R.string.session_summary_done),
                onDismiss = { viewModel.onIntent(MushafIntent.DismissSessionSummary) },
            )
        }

        micPrompt?.let { prompt ->
            val dismiss = {
                micPrompt = null
                viewModel.onIntent(MushafIntent.DismissCaptureError)
            }
            MicPromptDialog(
                prompt = prompt,
                onConfirm = {
                    dismiss()
                    when (prompt) {
                        MicPrompt.Preprompt ->
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        MicPrompt.Denied -> context.openAppSettings()
                        
                        MicPrompt.Unavailable, MicPrompt.ServiceUnreachable -> Unit
                    }
                },
                onDismiss = dismiss,
            )
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

        // ── Tafsir sheet ────────────────────────────────────────────────────────
        if (state.tafsirState !is com.example.mushaf.presentation.state.TafsirState.Idle) {
            val tafsirState = state.tafsirState
            com.example.mushaf.presentation.components.TafsirBottomSheet(
                tafsirState = tafsirState,
                onDismiss = { viewModel.onIntent(MushafIntent.DismissTafsir) },
                onRetry = if (tafsirState is com.example.mushaf.presentation.state.TafsirState.Error) {
                    {
                        // Re-trigger last LoadTafsir. Error state carries surah/ayah via Loading that preceded it.
                        // We use a simple approach: dismiss and ask user to long-press again.
                        viewModel.onIntent(MushafIntent.DismissTafsir)
                    }
                } else null,
            )
        }

        // ── User Guide Tooltip Overlay ──────────────────────────────────────────
        com.example.mushaf.presentation.guide.GuideTooltip(
            visible = state.showUserGuide,
            stepCounterText = stringResource(
                id = R.string.guide_step_counter,
                state.guideStep,
                com.example.mushaf.presentation.guide.MushafGuideStep.entries.size
            ),
            instructionText = stringResource(
                id = when (state.guideStep) {
                    1 -> R.string.guide_surah_name
                    2 -> R.string.guide_ayah_long_press
                    3 -> R.string.guide_mode_reading
                    4 -> R.string.guide_mode_listen
                    5 -> R.string.guide_mode_recitation
                    6 -> R.string.guide_mode_muallem
                    else -> R.string.guide_surah_name
                }
            ),
            nextButtonText = stringResource(
                id = if (state.guideStep >= 6) R.string.guide_got_it else R.string.guide_next
            ),
            anchor = when (state.guideStep) {
                1    -> com.example.mushaf.presentation.guide.TooltipAnchor.TOP
                2    -> com.example.mushaf.presentation.guide.TooltipAnchor.CENTER
                else -> com.example.mushaf.presentation.guide.TooltipAnchor.BOTTOM
            },
            onNext = { viewModel.onIntent(MushafIntent.GuideNextStep) },
            onDismiss = { viewModel.onIntent(MushafIntent.DismissGuide) }
        )
    }
}
