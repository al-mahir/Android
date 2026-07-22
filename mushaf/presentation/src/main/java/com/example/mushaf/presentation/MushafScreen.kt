package com.example.mushaf.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.components.MushafBottomBar
import com.example.mushaf.presentation.components.MushafErrorState
import com.example.mushaf.presentation.components.MushafLoading
import com.example.mushaf.presentation.components.MushafPageView
import com.example.mushaf.presentation.components.MushafTopBar
import com.example.designsystem.components.mushaf.AudioPlayerBar
import com.example.designsystem.components.mushaf.ReciterPickerSheet
import com.example.designsystem.components.mushaf.ReciterItem
import com.example.designsystem.components.mushaf.CorrectionsSheet
import com.example.mushaf.presentation.audio.AudioState
import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.presentation.recite.LiveSessionStatusRow
import com.example.mushaf.presentation.recite.CorrectionsUiMapper
import com.example.mushaf.presentation.recite.correctionsSubtitle
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
    /**
     * Page to open on, e.g. when arriving from Home's "Continue Reading". Null resumes the
     * reader's own persisted last page.
     */
    startPage: Int? = null,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
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

    // Derived once per feedback change rather than per page in the pager, which recomposes for
    // the neighbours on every swipe.
    val wordMarks by remember(state.liveCorrection.wordFeedback) {
        derivedStateOf { state.liveCorrection.wordFeedback.mapValues { (_, word) -> word.mark } }
    }
    val corrections by remember(state.liveCorrection.wordFeedback) {
        derivedStateOf { CorrectionsUiMapper.toCorrections(state.liveCorrection.wordFeedback) }
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
            // Live correction has no offline mode. Say so rather than letting the reciter read
            // an absence of corrections as an absence of mistakes.
            CaptureError.SERVICE_UNREACHABLE -> micPrompt = MicPrompt.ServiceUnreachable
            null -> Unit
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onScreenStopped()
    }

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
                                wordMarks = if (pageNumber == state.currentPage) wordMarks else emptyMap(),
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
            surahName = state.page?.lines
                ?.firstOrNull { it.surahNumber != null }
                ?.let { SurahNameResolver.nameFor(it.surahNumber) }
                ?: "",
            juzNumber = 1,
            hizbNumber = 1,
            isBookmarked = false,
            onBack = onBack,
            onBookmark = {},
            onSettings = onOpenSettings,
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
                micLevel = state.micLevel,
                // Inside the bar, not floating over the page: the muṣḥaf stays fully readable
                // and the pills hide with the rest of the chrome on a tap.
                statusRow = if (state.mushafMode == MushafMode.RECITATION) {
                    {
                        LiveSessionStatusRow(
                            live = state.liveCorrection,
                            isRecording = state.isRecordingActive,
                            onCorrectionsClick = { showCorrections = true },
                        )
                    }
                } else {
                    null
                },
                onToggleRecording = {
                    when {
                        // Stopping never needs a permission check.
                        state.isRecordingActive -> viewModel.onIntent(MushafIntent.ToggleRecording)
                        context.hasRecordAudioPermission() ->
                            viewModel.onIntent(MushafIntent.ToggleRecording)
                        // Explainer first, OS dialog second — never the other way round.
                        else -> micPrompt = MicPrompt.Preprompt
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showCorrections) {
            CorrectionsSheet(
                title = stringResource(R.string.mushaf_corrections_title),
                subtitle = state.liveCorrection.correctionsSubtitle(),
                corrections = corrections.map { it.toCard() },
                emptyMessage = stringResource(R.string.mushaf_corrections_empty),
                onDismiss = { showCorrections = false },
                onMistakeClick = { wordId ->
                    // Close and jump to the word, so the correction is read against the page it
                    // happened on rather than as a bare word in a list.
                    viewModel.onIntent(MushafIntent.HighlightWord(wordId))
                    viewModel.onIntent(MushafIntent.SelectMistake(wordId))
                    showCorrections = false
                },
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
                        // Acknowledge only; retrying is the mic button, which is still there.
                        MicPrompt.Unavailable, MicPrompt.ServiceUnreachable -> Unit
                    }
                },
                onDismiss = dismiss,
            )
        }
        
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
    }
}
