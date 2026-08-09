package com.example.mushaf.presentation

import android.Manifest
import android.R.attr.animation
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.designsystem.components.mushaf.AudioPlayerBar
import com.example.mushaf.presentation.download.components.resolve
import com.example.designsystem.components.mushaf.ReciterItem
import com.example.designsystem.components.mushaf.ReciterPickerSheet
import com.example.designsystem.components.mushaf.SurahItem
import com.example.designsystem.components.mushaf.SurahPickerSheet
import com.example.designsystem.components.mushaf.TajweedLegendSheet
import com.example.designsystem.components.mushaf.CorrectionsSheet
import com.example.designsystem.components.session.SessionSummarySheet
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.domain.model.SurahCatalog
import com.example.mushaf.domain.model.SurahOrigin
import com.example.mushaf.presentation.audio.AudioState
import com.example.mushaf.presentation.components.GradingModeToggle
import com.example.mushaf.presentation.components.MushafBottomBar
import com.example.mushaf.presentation.components.MushafErrorState
import com.example.mushaf.presentation.components.MushafLoading
import com.example.mushaf.presentation.components.MushafPageOverlay
import com.example.mushaf.presentation.components.MushafPageView
import com.example.mushaf.presentation.components.MushafTopBar
import com.example.mushaf.presentation.muallem.MuallemPhase
import com.example.mushaf.presentation.muallem.MuallemSessionBar
import com.example.mushaf.presentation.muallem.MuallemSetupSheet
import com.example.mushaf.presentation.recite.LiveSessionStatusRow
import com.example.mushaf.presentation.recite.CorrectionFilter
import com.example.mushaf.presentation.recite.CorrectionsUiMapper
import com.example.mushaf.presentation.recite.correctionsSubtitle
import com.example.mushaf.presentation.recite.phonemesCard
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
    openInListenMode: Boolean = false,
    onBack: () -> Unit = {},
    onNavigateSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onNavigateToSurahDownload: (Int) -> Unit = {},
    onSearchClick: () -> Unit = {},
    viewModel: MushafViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(openInListenMode) {
        if (openInListenMode) {
            viewModel.onIntent(MushafIntent.SetMode(MushafMode.LISTEN))
        }
    }

    val context = LocalContext.current

    com.example.mushaf.presentation.core.mvi.ObserveEffect(viewModel.effects) { effect ->
        when (effect) {
            is com.example.mushaf.presentation.state.MushafEffect.ShowMessage ->
                android.widget.Toast.makeText(context, effect.messageRes, android.widget.Toast.LENGTH_SHORT).show()
            com.example.mushaf.presentation.state.MushafEffect.NavigateBack -> onBack()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = state.currentPage - 1,
        pageCount = { state.pageCount },
    )

    var showReciterPicker by remember { mutableStateOf(false) }

    var topBarHeightPx by remember { mutableIntStateOf(0) }

    val downloadsViewModel: com.example.mushaf.presentation.download.DownloadsViewModel = 
        org.koin.androidx.compose.koinViewModel(
            key = com.example.mushaf.domain.model.ResourceKind.RECITER.name,
            parameters = { org.koin.core.parameter.parametersOf(com.example.mushaf.domain.model.ResourceKind.RECITER) }
        )
    val downloadsState by downloadsViewModel.state.collectAsStateWithLifecycle()

    com.example.mushaf.presentation.core.mvi.ObserveEffect(downloadsViewModel.effect) { effect ->
        when (effect) {
            is com.example.mushaf.presentation.download.state.DownloadsEffect.NavigateToSurahList -> {
                showReciterPicker = false
                effect.reciterId.toIntOrNull()?.let { onNavigateToSurahDownload(it) }
            }
            is com.example.mushaf.presentation.download.state.DownloadsEffect.ShowMessage -> {
                android.widget.Toast.makeText(context, effect.messageRes, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

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

                            MushafPageOverlay(
                                pageNumber = pageNumber,
                                juzNumber = MushafConstants.juzForPage(pageNumber),
                                hizbQuarterInHizb = ((MushafConstants.hizbQuarterForPage(pageNumber) - 1) % 4) + 1,
                                isRightPage = pageNumber % 2 == 1,
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
            isBookmarked = state.isCurrentPageBookmarked,
            onBack = onBack,
            onBookmark = { viewModel.onIntent(MushafIntent.TogglePageBookmark) },
            onSettings = onOpenSettings,
            onSurahNameClick = { viewModel.onIntent(MushafIntent.ShowSurahPicker) },
            onSearchClick = onSearchClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { topBarHeightPx = it.size.height },
        )

        var bottomBarHeightPx by remember { mutableIntStateOf(0) }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { bottomBarHeightPx = it.size.height },
        ) {
            AnimatedVisibility(
                visible = state.mushafMode == MushafMode.LISTEN && state.areBarsVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                AudioPlayerBar(
                    isPlaying = state.audioState == AudioState.PLAYING,
                    reciterName = state.currentReciter?.let { com.example.mushaf.domain.model.LocalizedText(arabic = it.nameArabic, english = it.name).resolve() } ?: "",
                    playbackSpeed = state.playbackSpeed,
                    onPlayPauseClick = { viewModel.onIntent(MushafIntent.PlayPauseAudio) },
                    onNextClick = { viewModel.onIntent(MushafIntent.NextSurahAudio) },
                    onPrevClick = { viewModel.onIntent(MushafIntent.PrevSurahAudio) },
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
                isMicEnabled = state.isMicEnabled,
                onToggleAyahVisibility = { viewModel.onIntent(MushafIntent.ToggleAyahVisibility) },
                onRevealNextWord = { viewModel.onIntent(MushafIntent.RevealNextWord) },
                onRevealNextAyah = { viewModel.onIntent(MushafIntent.RevealNextAyah) },
                onModeSelected = { mode -> viewModel.onIntent(MushafIntent.SetMode(mode)) },
                micLevel = state.micLevel,
                canFinishSession = state.isRecordingActive && state.liveCorrection.isActive,
                onFinishSession = { viewModel.onIntent(MushafIntent.FinishAndStartNewSession) },


                muallemBar = state.muallemSession?.let { session ->
                    {
                        MuallemSessionBar(
                            session = session,
                            isConnecting = state.liveCorrection.isConnecting,
                            isActive = state.liveCorrection.isActive,
                            onStopSession = { viewModel.onIntent(MushafIntent.StopMuallemSession) },
                        )
                    }
                },
                statusRow = if (state.mushafMode == MushafMode.RECITATION || (state.mushafMode == MushafMode.MUALLEM && state.muallemSession?.isSessionActive == true)) {
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
                        // In Mu'allem mode, mic button ends the current recording
                        state.mushafMode == MushafMode.MUALLEM &&
                            state.muallemSession?.phase is MuallemPhase.UserRecording -> {
                            viewModel.onIntent(MushafIntent.MuallemRepeatDone)
                        }
                        state.isRecordingActive -> viewModel.onIntent(MushafIntent.ToggleRecording)
                        context.hasRecordAudioPermission() ->
                            viewModel.onIntent(MushafIntent.ToggleRecording)
                        else -> micPrompt = MicPrompt.Preprompt
                    }
                }
            )
        }

        // ── Tajweed Legend FAB ──────────────────────────────────────────────────
        // Ride on the measured bar height: the bar grows and shrinks with the audio player, the
        // status row, the grading toggle and the reveal actions, so a fixed offset would overlap.
        val bottomBarHeight = with(LocalDensity.current) { bottomBarHeightPx.toDp() }
        val fabBottomPadding by animateDpAsState(
            targetValue = bottomBarHeight + 12.dp,
            label = "fabBottomPadding"
        )

        AnimatedVisibility(
            visible = state.areBarsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
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
                    painter = androidx.compose.ui.res.painterResource(com.example.designsystem.R.drawable.ic_palette),
                    contentDescription = stringResource(R.string.mushaf_cd_tajweed_legend),
                )
            }
        }

    var dismissedFeedbackRepeatIndex by remember { mutableIntStateOf(-1) }
    val currentFeedbackRepeat = (state.muallemSession?.phase as? MuallemPhase.ShowingFeedback)?.repeatIndex ?: -1
    val autoShowFeedback = state.mushafMode == MushafMode.MUALLEM &&
            currentFeedbackRepeat != -1 &&
            dismissedFeedbackRepeatIndex != currentFeedbackRepeat

    if (showCorrections || autoShowFeedback) {
        val sheetTitle = if (autoShowFeedback) {
            stringResource(
                R.string.muallem_feedback_repeat_title,
                stringResource(R.string.mushaf_corrections_title),
                currentFeedbackRepeat,
                state.muallemSession?.repeatCount ?: 1,
            )
        } else {
            stringResource(R.string.mushaf_corrections_title)
        }
        CorrectionsSheet(
            title = sheetTitle,
            subtitle = state.liveCorrection.correctionsSubtitle(),
            corrections = visibleCorrections.map { it.toCard() },
            tabs = correctionTabs.map { it.toChip() },
            selectedTabIndex = correctionTabs.indexOf(selectedTab).coerceAtLeast(0),
            phonemes = state.liveCorrection.phonemesCard(),
            onTabSelected = { correctionTabIndex = it },
            emptyMessage = stringResource(R.string.mushaf_corrections_empty),
            practiceTitle = stringResource(R.string.mushaf_practice_focus_title),
            practiceFocus = state.liveCorrection.practiceFocus.map { it.label() },
            onDismiss = {
                showCorrections = false
                if (currentFeedbackRepeat != -1) {
                    dismissedFeedbackRepeatIndex = currentFeedbackRepeat
                }
            },
            onMistakeClick = { wordId ->
                viewModel.onIntent(MushafIntent.HighlightWord(wordId))
                viewModel.onIntent(MushafIntent.SelectMistake(wordId))
                showCorrections = false
                if (currentFeedbackRepeat != -1) {
                    dismissedFeedbackRepeatIndex = currentFeedbackRepeat
                }
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
            com.example.mushaf.presentation.components.MushafReciterPickerSheet(
                reciters = state.availableReciters,
                downloadableResources = downloadsState.resources,
                selectedId = state.currentReciter?.id,
                onReciterSelected = { domainReciter ->
                    viewModel.onIntent(MushafIntent.SelectReciter(domainReciter))
                    showReciterPicker = false
                },
                onDownloadClick = { id ->
                    downloadsViewModel.onIntent(com.example.mushaf.presentation.download.state.DownloadsIntent.DownloadClicked(id))
                },
                onCancelClick = { id ->
                    downloadsViewModel.onIntent(com.example.mushaf.presentation.download.state.DownloadsIntent.CancelClicked(id))
                },
                onDeleteClick = { id ->
                    downloadsViewModel.onIntent(com.example.mushaf.presentation.download.state.DownloadsIntent.DeleteClicked(id))
                },
                onDismiss = { showReciterPicker = false },
            )
            
            // ── Download Options Dialog (Entire Quran / Select Surahs) ──────────
            downloadsState.pendingDownloadOptions?.let { _ ->
                com.example.designsystem.components.dialog.ConfirmationDialog(
                    title = stringResource(R.string.download_dialog_title),
                    message = stringResource(R.string.download_dialog_message),
                    confirmLabel = stringResource(R.string.download_dialog_entire_quran),
                    dismissLabel = stringResource(R.string.download_dialog_select_surahs),
                    onConfirm = {
                        downloadsViewModel.onIntent(com.example.mushaf.presentation.download.state.DownloadsIntent.DownloadOptionsFullQuran)
                    },
                    onDismiss = {
                        downloadsViewModel.onIntent(com.example.mushaf.presentation.download.state.DownloadsIntent.DownloadOptionsSurahs)
                    },
                    onDismissRequest = {
                        downloadsViewModel.onIntent(com.example.mushaf.presentation.download.state.DownloadsIntent.DownloadOptionsDismissed)
                    }
                )
            }

            // Re-use the pending full download and deletion dialogs from DownloadsScreen
            downloadsState.pendingFullDownload?.let { target ->
                val formattedSize = com.example.mushaf.presentation.download.components.rememberFormattedSize(target.sizeBytes)
                com.example.designsystem.components.dialog.ConfirmationDialog(
                    title = stringResource(R.string.downloads_full_quran_title),
                    message = stringResource(R.string.downloads_full_quran_message, target.name.resolve(), formattedSize),
                    confirmLabel = stringResource(R.string.downloads_action_download),
                    dismissLabel = stringResource(R.string.downloads_delete_cancel),
                    onConfirm = { downloadsViewModel.onIntent(com.example.mushaf.presentation.download.state.DownloadsIntent.DownloadFullConfirmed) },
                    onDismiss = { downloadsViewModel.onIntent(com.example.mushaf.presentation.download.state.DownloadsIntent.DownloadFullDismissed) },
                    confirmColor = Theme.colors.primary,
                    confirmContentColor = Theme.colors.onPrimary
                )
            }
            downloadsState.pendingDeletion?.let { target ->
                com.example.designsystem.components.dialog.ConfirmationDialog(
                    title = stringResource(R.string.downloads_delete_title),
                    message = stringResource(R.string.downloads_delete_message, target.name.resolve()),
                    confirmLabel = stringResource(R.string.downloads_delete_confirm),
                    dismissLabel = stringResource(R.string.downloads_delete_cancel),
                    onConfirm = { downloadsViewModel.onIntent(com.example.mushaf.presentation.download.state.DownloadsIntent.DeleteConfirmed) },
                    onDismiss = { downloadsViewModel.onIntent(com.example.mushaf.presentation.download.state.DownloadsIntent.DeleteDismissed) },
                    confirmColor = Theme.colors.error,
                    confirmContentColor = Theme.colors.onError
                )
            }
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

        // ── Mu'allem Setup sheet ────────────────────────────────────────────────
        if (state.showMuallemSetup) {
            MuallemSetupSheet(
                isOffline = state.isOffline,
                onDismiss = { viewModel.onIntent(MushafIntent.DismissMuallemSetup) },
                onConfirm = { surah, startAyah, endAyah, repeatCount, difficulty ->
                    viewModel.onIntent(MushafIntent.StartMuallemSession(surah, startAyah, endAyah, repeatCount, difficulty))
                },
            )
        }

        // ── Tafsir sheet ────────────────────────────────────────────────────────
        if (state.tafsirState !is com.example.mushaf.presentation.state.TafsirState.Idle) {
            val tafsirState = state.tafsirState
            val isAyahBookmarked = (tafsirState as? com.example.mushaf.presentation.state.TafsirState.Success)
                ?.let { it.tafsir.surahNumber to it.tafsir.ayahNumber }
                ?.let { it in state.bookmarkedAyahs }
                ?: false
            com.example.mushaf.presentation.components.TafsirBottomSheet(
                tafsirState = tafsirState,
                availableBooks = state.availableTafsirBooks,
                selectedKey = state.selectedTafsirKey,
                isAyahBookmarked = isAyahBookmarked,
                onDismiss = { viewModel.onIntent(MushafIntent.DismissTafsir) },
                onRetry = if (tafsirState is com.example.mushaf.presentation.state.TafsirState.Error) {
                    {
                        viewModel.onIntent(MushafIntent.DismissTafsir)
                    }
                } else null,
                onChangeTafsir = { viewModel.onIntent(MushafIntent.ChangeTafsirSource(it)) },
                onDownloadTafsir = { key, url -> viewModel.onIntent(MushafIntent.DownloadTafsir(key, url)) },
                onDeleteTafsir = { viewModel.onIntent(MushafIntent.DeleteTafsir(it)) },
                onBookmarkAyah = {
                    if (tafsirState is com.example.mushaf.presentation.state.TafsirState.Success) {
                        viewModel.onIntent(MushafIntent.ToggleAyahBookmark(
                            surah = tafsirState.tafsir.surahNumber,
                            ayah = tafsirState.tafsir.ayahNumber
                        ))
                    }
                }
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
