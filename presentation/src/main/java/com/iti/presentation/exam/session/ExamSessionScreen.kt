package com.iti.presentation.exam.session

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.button.SecondaryButton
import com.example.designsystem.components.card.InnerContentCard
import com.example.designsystem.components.dialog.ConfirmationDialog
import com.example.designsystem.components.section.SectionHeader
import com.example.designsystem.theme.Theme
import com.iti.domain.model.exam.ExamMistake
import com.iti.domain.model.exam.ExamMistakeCategory
import com.iti.domain.model.exam.ExamQuestion
import com.iti.domain.model.exam.QuestionStatus
import com.iti.domain.model.exam.WordFeedback
import com.iti.domain.model.exam.WordStatus
import com.iti.domain.model.quran.SurahNames
import com.iti.presentation.R
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.exam.session.state.ExamSessionEffect
import com.iti.presentation.exam.session.state.ExamSessionIntent
import com.iti.presentation.exam.session.state.ExamSessionUiState
import org.koin.androidx.compose.koinViewModel

// Updated High-Contrast Colors for clear reading
private val ColorCorrect  = Color(0xFF16A34A) // Darker Green
private val ColorAlmost   = Color(0xFFD97706) // Darker Amber
private val ColorError    = Color(0xFFDC2626) // Darker Red
private val ColorTrimmed  = Color(0xFF94A3B8) // Slate 400
private val ColorPending  = Color(0xFF2563EB) // Bright Blue for processing words
private val ColorHintText = Color(0xFF94A3B8) // Slate 400 for hidden/upcoming words

private val DotPending        = Color(0xFFCBD5E1)
private val DotCorrect        = Color(0xFF22C55E)
private val DotTajweed        = Color(0xFFF59E0B)
private val DotWordMistake    = Color(0xFFEF4444)
private val DotSkipped        = Color(0xFF94A3B8)

private const val AUDIO_SAMPLE_RATE = 16_000
private const val AUDIO_BUFFER_MS   = 100

@Composable
fun ExamSessionScreen(
    viewModel: ExamSessionViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSummary: (summaryId: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val globalMistakes by viewModel.globalMistakes.collectAsStateWithLifecycle()

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.onIntent(ExamSessionIntent.StartRecording)
    }

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            ExamSessionEffect.NavigateBack            -> onNavigateBack()
            is ExamSessionEffect.NavigateToSummary   -> onNavigateToSummary(effect.summaryId)
            ExamSessionEffect.PlayStopSound           -> { }
            ExamSessionEffect.RequestMicrophonePermission -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (state.isRecording) {
        AudioCaptureEffect(onChunk = { pcm ->
            viewModel.onIntent(ExamSessionIntent.AudioPcmCaptured(pcm))
        })
    }

    ExamSessionContent(
        state = state,
        globalMistakes = globalMistakes,
        onIntent = { intent ->
            if (intent == ExamSessionIntent.StartRecording) micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            else viewModel.onIntent(intent)
        }
    )
}

@Composable
private fun AudioCaptureEffect(onChunk: (ByteArray) -> Unit) {
    DisposableEffect(Unit) {
        val minBuf = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufSize = (AUDIO_SAMPLE_RATE * AUDIO_BUFFER_MS / 1000 * 2).coerceAtLeast(minBuf)

        val recorder = try {
            val audioRecord = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize)
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) { audioRecord.release(); null } else audioRecord
        } catch (e: SecurityException) { null }

        if (recorder != null) {
            try { recorder.startRecording() } catch (e: Exception) { recorder.release(); return@DisposableEffect onDispose {} }
        }

        val thread = Thread {
            if (recorder == null) return@Thread
            val buffer = ByteArray(bufSize)
            while (!Thread.currentThread().isInterrupted && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) onChunk(buffer.copyOf(read))
            }
        }.also { it.start() }

        onDispose { thread.interrupt(); recorder?.stop(); recorder?.release() }
    }
}

@Composable
private fun ExamSessionContent(
    state: ExamSessionUiState,
    globalMistakes: List<ExamMistake>,
    onIntent: (ExamSessionIntent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.backGround)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ExamTopBar(
                currentIndex = state.currentQuestionIndex + 1,
                total        = state.totalQuestions.coerceAtLeast(1),
                mistakeCount = globalMistakes.size,
                onEndClicked = { onIntent(ExamSessionIntent.EndSessionClicked) },
                onMistakesClicked = { onIntent(ExamSessionIntent.ShowMistakesSheet) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            QuestionProgressBar(
                statuses = state.allQuestionStatuses,
                currentIndex = state.currentQuestionIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.isPreparing     -> PreparingCard()
                    state.hasConnectionError -> ConnectionErrorCard(onRetry = { onIntent(ExamSessionIntent.StartRecording) })
                    state.lastFeedbackStatus == "ambiguous" -> AmbiguousCard()
                    state.lastFeedbackStatus == "no_match"  -> NoMatchCard()
                    else -> AyahQuestionCard(
                        question        = state.currentQuestion,
                        words           = state.activeQuestionResult?.words ?: emptyList(),
                        currentSurah    = state.currentRecitationSurah,
                        currentAyah     = state.currentRecitationAyah,
                        currentWord     = state.currentRecitationWord,
                        isConnecting    = state.isConnecting,
                        isRecording     = state.isRecording,
                        elapsedSeconds  = state.elapsedSeconds,
                        revealedWordsCount = state.revealedWordsCount,
                        showFullHint    = state.showFullHint,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TrackerRow(
                currentSurah = state.currentRecitationSurah,
                currentAyah = state.currentRecitationAyah,
                fallbackSurah = state.currentQuestion?.surahNumber ?: 1,
                fallbackAyah = state.currentQuestion?.ayahNumber ?: 1,
                isVisible = state.isTrackerVisible,
                onToggle = { onIntent(ExamSessionIntent.ToggleTrackerVisibility) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            BottomControls(
                isRecording  = state.isRecording,
                isConnecting = state.isConnecting,
                isPreparing  = state.isPreparing || state.countdownSeconds != null,
                isReviewing  = state.isReviewing,
                showFullHint = state.showFullHint,
                onStartRecord = { onIntent(ExamSessionIntent.StartRecording) },
                onStopRecord  = { onIntent(ExamSessionIntent.StopRecording) },
                onSkip        = { onIntent(ExamSessionIntent.SkipQuestion) },
                onNextQuestion = { onIntent(ExamSessionIntent.NextQuestionClicked) },
                onRevealNextWord = { onIntent(ExamSessionIntent.RevealNextWord) },
                onRevealFullHint = { onIntent(ExamSessionIntent.RevealFullHint) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            )
        }

        AnimatedVisibility(
            visible = state.showHasbuk,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Theme.colors.primary.copy(alpha = 0.9f))
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.exam_hasbok),
                    style = Theme.typography.title,
                    color = Color.White
                )
            }
        }

        AnimatedVisibility(
            visible = state.countdownSeconds != null,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.countdownSeconds?.toString() ?: "",
                    style = Theme.typography.display.copy(fontSize = 120.sp, color = Color.White, fontWeight = FontWeight.Bold)
                )
            }
        }

        if (state.showEndConfirmDialog) {
            ConfirmationDialog(
                title        = stringResource(R.string.exam_end_session_confirm_title),
                message      = stringResource(R.string.exam_end_session_confirm_message),
                confirmLabel = stringResource(R.string.exam_end_session_confirm),
                dismissLabel = stringResource(android.R.string.cancel),
                onConfirm    = { onIntent(ExamSessionIntent.EndSessionConfirmed) },
                onDismiss    = { onIntent(ExamSessionIntent.EndSessionConfirmDismissed) },
            )
        }

        if (state.showMistakesSheet) {
            MistakesBottomSheet(
                mistakes = globalMistakes,
                onDismiss = { onIntent(ExamSessionIntent.DismissMistakesSheet) },
            )
        }
    }
}

@Composable
private fun ExamTopBar(
    currentIndex: Int,
    total: Int,
    mistakeCount: Int,
    onEndClicked: () -> Unit,
    onMistakesClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.exam_question_progress, currentIndex, total),
            style = Theme.typography.title,
            color = Theme.colors.onSurface,
        )

        if (mistakeCount > 0) {
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = Theme.colors.error,
                        contentColor   = Color.White,
                    ) {
                        Text(
                            text  = mistakeCount.toString(),
                            style = Theme.typography.body.small.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            ) {
                IconButton(onClick = onMistakesClicked) {
                    Icon(
                        imageVector        = Icons.Rounded.ErrorOutline,
                        contentDescription = stringResource(R.string.exam_mistakes_sheet_title),
                        tint               = Theme.colors.error,
                        modifier           = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionProgressBar(statuses: List<QuestionStatus>, currentIndex: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        statuses.forEachIndexed { idx, status ->
            val isActive = idx == currentIndex
            val color = when (status) {
                QuestionStatus.PENDING         -> if (isActive) Theme.colors.primary else DotPending
                QuestionStatus.CORRECT         -> DotCorrect
                QuestionStatus.TAJWEED_MISTAKE -> DotTajweed
                QuestionStatus.WORD_MISTAKE    -> DotWordMistake
                QuestionStatus.SKIPPED         -> DotSkipped
            }
            val animatedHeight by animateFloatAsState(targetValue = if (isActive) 8f else 5f, label = "dot_height")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

@Composable
private fun TrackerRow(
    currentSurah: Int?,
    currentAyah: Int?,
    fallbackSurah: Int,
    fallbackAyah: Int,
    isVisible: Boolean,
    onToggle: () -> Unit
) {
    val displaySurah = currentSurah ?: fallbackSurah
    val displayAyah = currentAyah ?: fallbackAyah
    val surahName = SurahNames.nameOf(displaySurah) ?: "سورة $displaySurah"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                contentDescription = "Toggle Tracker",
                tint = Theme.colors.primary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        AnimatedContent(targetState = isVisible, label = "tracker") { visible ->
            if (visible) {
                Text(
                    text = "$surahName · الآية $displayAyah",
                    style = Theme.typography.title,
                    color = Theme.colors.onSurface
                )
            } else {
                Text(
                    text = "سورة — · الآية —",
                    style = Theme.typography.title,
                    color = Theme.colors.hint
                )
            }
        }
    }
}

@Composable
private fun AyahQuestionCard(
    question: ExamQuestion?,
    words: List<WordFeedback>,
    currentSurah: Int?,
    currentAyah: Int?,
    currentWord: Int?,
    isConnecting: Boolean,
    isRecording: Boolean,
    elapsedSeconds: Int,
    revealedWordsCount: Int,
    showFullHint: Boolean,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Theme.colors.surface)
            .border(1.dp, Theme.colors.border, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (question != null) {
                val lastGradedIdx = words.indexOfLast { it.status != WordStatus.PENDING }

                val progressAbsIdx = words.indexOfFirst {
                    it.surah == currentSurah && it.ayah == currentAyah && it.wordIdx == currentWord
                }.coerceAtLeast(0)

                val maxSpokenIdx = maxOf(progressAbsIdx, lastGradedIdx)

                val hintSize = 3
                val spokenRevealLimit = if (lastGradedIdx >= 0) lastGradedIdx + 1 else 0
                val baseReveal = maxOf(hintSize, spokenRevealLimit + revealedWordsCount)

                val revealLimit = if (showFullHint) {
                    words.size
                } else {
                    baseReveal.coerceAtMost(words.size)
                }

                // Auto-scroll to the bottom smoothly as new words appear
                LaunchedEffect(revealLimit) {
                    scrollState.animateScrollTo(
                        scrollState.maxValue,
                        animationSpec = tween(durationMillis = 600, easing = LinearEasing)
                    )
                }

                // Scrollable area with top & bottom fade (Movie Credits Effect)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.15f to Color.Black,
                                    0.85f to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(vertical = 48.dp), // Extra padding allows text to slide out of the fade zone gracefully
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        AnimatedContent(
                            targetState = Pair(words, revealLimit),
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                            label = "unified_text",
                        ) { (currentWords, limit) ->
                            val unifiedText = buildAnnotatedString {
                                currentWords.take(limit).forEachIndexed { index, wordFeedback ->
                                    val color = when {
                                        wordFeedback.status == WordStatus.CORRECT -> ColorCorrect
                                        wordFeedback.status == WordStatus.ALMOST  -> ColorAlmost
                                        wordFeedback.status == WordStatus.ERROR   -> ColorError
                                        wordFeedback.status == WordStatus.TRIMMED -> ColorTrimmed
                                        index <= maxSpokenIdx -> ColorPending
                                        else -> ColorHintText
                                    }
                                    withStyle(SpanStyle(color = color)) {
                                        append(wordFeedback.uthmani)
                                    }
                                    append(" ")
                                }

                                if (!showFullHint && limit < currentWords.size) {
                                    withStyle(SpanStyle(color = ColorHintText.copy(alpha = 0.5f))) {
                                        append("...")
                                    }
                                }
                            }

                            Text(
                                text = if (currentWords.isEmpty()) androidx.compose.ui.text.AnnotatedString("...") else unifiedText,
                                style = Theme.typography.display.copy(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 52.sp,
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Bottom indicators section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isConnecting -> ConnectingIndicator()
                    isRecording  -> RecordingTimerRow(elapsedSeconds = elapsedSeconds)
                    else -> Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ConnectingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "connecting_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "connecting_alpha",
    )
    Text(
        text = stringResource(R.string.exam_connecting),
        style = Theme.typography.body.medium.copy(color = Theme.colors.primary.copy(alpha = alpha)),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RecordingTimerRow(elapsedSeconds: Int) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f, targetValue = 1.2f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "rec_dot_scale",
        )
        Box(
            modifier = Modifier.size(8.dp).scale(scale).clip(CircleShape).background(Theme.colors.error),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            style = Theme.typography.body.medium.copy(fontWeight = FontWeight.Medium, color = Theme.colors.onSurface),
        )
    }
}

@Composable
private fun PreparingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Theme.colors.surface)
            .border(1.dp, Theme.colors.border, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        ConnectingIndicator()
    }
}

@Composable
private fun ConnectionErrorCard(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Theme.colors.surface)
            .border(1.dp, Theme.colors.border, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.exam_connection_error),
                style = Theme.typography.body.large, color = Theme.colors.error,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp),
            )
            SecondaryButton(caption = stringResource(R.string.exam_retry), onClick  = onRetry)
        }
    }
}

@Composable
private fun AmbiguousCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Theme.colors.surface)
            .border(1.dp, Theme.colors.border, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.exam_ambiguous_passage),
            style = Theme.typography.body.large, color = Theme.colors.hint,
            textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun NoMatchCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Theme.colors.surface)
            .border(1.dp, Theme.colors.border, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.exam_no_match),
            style = Theme.typography.body.large, color = Theme.colors.hint,
            textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun BottomControls(
    isRecording: Boolean,
    isConnecting: Boolean,
    isPreparing: Boolean,
    isReviewing: Boolean,
    showFullHint: Boolean,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onSkip: () -> Unit,
    onNextQuestion: () -> Unit,
    onRevealNextWord: () -> Unit,
    onRevealFullHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val disabled = isPreparing

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (!isReviewing && !showFullHint) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SecondaryButton(
                    caption = stringResource(R.string.exam_reveal_rest_of_ayah),
                    onClick = onRevealFullHint,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    caption = stringResource(R.string.exam_reveal_next_word),
                    onClick = onRevealNextWord,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (isReviewing) {
            PrimaryButton(
                caption = stringResource(R.string.exam_next_question),
                onClick = onNextQuestion,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            )
        } else {
            val micScale by animateFloatAsState(targetValue = if (isRecording) 1.12f else 1f, label = "mic_scale")
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(micScale)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) Theme.colors.error
                        else if (disabled || isConnecting) Theme.colors.surfaceVariant
                        else Theme.colors.primary
                    )
                    .then(if (!disabled && !isConnecting) Modifier else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.IconButton(
                    onClick = { if (isRecording) onStopRecord() else onStartRecord() },
                    enabled = !disabled && !isConnecting,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = "Mic", tint = Color.White, modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!isReviewing) {
            PrimaryButton(
                caption = stringResource(R.string.exam_skip_question),
                onClick = onSkip,
                isDisabled = disabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Mistakes bottom sheet
// ---------------------------------------------------------------------------

@Composable
private fun MistakesBottomSheet(
    mistakes: List<ExamMistake>,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        SectionHeader(
            title = stringResource(R.string.exam_mistakes_sheet_title),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Theme.spacing.small),
        )

        if (mistakes.isEmpty()) {
            Text(
                text = stringResource(R.string.exam_no_mistakes_yet),
                style = Theme.typography.body.medium,
                color = Theme.colors.hint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Theme.spacing.large),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
                modifier = Modifier.padding(bottom = Theme.spacing.large),
            ) {
                items(mistakes) { mistake ->
                    SheetMistakeItem(mistake = mistake)
                }
            }
        }
    }
}

@Composable
private fun SheetMistakeItem(mistake: ExamMistake) {
    InnerContentCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
        ) {
            // Surah + ayah location
            Text(
                text = "سورة ${SurahNames.nameOf(mistake.surahNumber) ?: ""} · الآية ${mistake.ayahNumber}",
                style = Theme.typography.body.small,
                color = Theme.colors.hint,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Expected word (what should have been said)
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.exam_mistake_expected),
                        style = Theme.typography.body.small,
                        color = Theme.colors.hint,
                    )
                    Text(
                        text = mistake.word,
                        style = Theme.typography.body.large.copy(fontWeight = FontWeight.Bold),
                        color = Theme.colors.onSurface,
                    )
                }

                // Said word (what was actually said) — only shown when available
                val predictedWord = mistake.predictedText
                if (!predictedWord.isNullOrBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.exam_mistake_said),
                            style = Theme.typography.body.small,
                            color = Theme.colors.hint,
                        )
                        Text(
                            text = predictedWord,
                            style = Theme.typography.body.large.copy(fontWeight = FontWeight.Bold),
                            color = ColorError,
                        )
                    }
                }
            }

            // Category chip
            Box(
                modifier = Modifier
                    .background(
                        mistakeCategoryColor(mistake.category).copy(alpha = 0.12f),
                        Theme.shapes.small,
                    )
                    .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.extraSmall)
            ) {
                Text(
                    text = mistakeCategoryLabel(mistake.category),
                    style = Theme.typography.body.small.copy(fontWeight = FontWeight.SemiBold),
                    color = mistakeCategoryColor(mistake.category),
                )
            }
        }
    }
}

@Composable
private fun mistakeCategoryLabel(category: ExamMistakeCategory): String = when (category) {
    ExamMistakeCategory.MEMORIZATION -> stringResource(R.string.exam_mistake_category_memorization)
    ExamMistakeCategory.TASHKEEL    -> stringResource(R.string.exam_mistake_category_tashkeel)
    ExamMistakeCategory.TAJWEED     -> stringResource(R.string.exam_mistake_category_tajweed)
    ExamMistakeCategory.OTHER       -> stringResource(R.string.exam_mistake_category_other)
}

private fun mistakeCategoryColor(category: ExamMistakeCategory): Color = when (category) {
    ExamMistakeCategory.MEMORIZATION -> ColorError
    ExamMistakeCategory.TASHKEEL    -> ColorAlmost
    ExamMistakeCategory.TAJWEED     -> Color(0xFF8B5CF6)
    ExamMistakeCategory.OTHER       -> ColorTrimmed
}