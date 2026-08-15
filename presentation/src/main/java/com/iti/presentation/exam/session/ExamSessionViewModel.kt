package com.iti.presentation.exam.session

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.usecase.GetPageUseCase
import com.example.mushaf.domain.usecase.GetTargetPageUseCase
import com.iti.domain.model.exam.ExamMistake
import com.iti.domain.model.exam.ExamQuestion
import com.iti.domain.model.exam.ExamQuestionResult
import com.iti.domain.model.exam.ExamScope
import com.iti.domain.model.exam.ExamSummary
import com.iti.domain.model.exam.WordFeedback
import com.iti.domain.model.exam.WordStatus
import com.iti.domain.repository.exam.ExamDataSource
import com.iti.domain.repository.exam.ExamSessionHandle
import com.iti.domain.repository.exam.FeedbackEvent
import com.iti.domain.usecase.exam.GenerateExamQuestionsUseCase
import com.iti.domain.usecase.exam.SaveExamSummaryUseCase
import com.iti.domain.usecase.exam.classifyMistake
import com.example.mushaf.domain.repository.MushafRepository
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.iti.domain.core.getOrNull
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.exam.session.state.ExamSessionEffect
import com.iti.presentation.exam.session.state.ExamSessionIntent
import com.iti.presentation.exam.session.state.ExamSessionUiState
import com.iti.presentation.exam.summary.ExamSessionCache
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "ExamSessionViewModel"

class ExamSessionViewModel(
    private val scope: ExamScope,
    private val questionCount: Int,
    private val linesPerQuestion: Int,
    private val generateQuestions: GenerateExamQuestionsUseCase,
    private val saveSummary: SaveExamSummaryUseCase,
    private val dataSource: ExamDataSource,
    private val mushafRepository: MushafRepository,
    private val getPage: GetPageUseCase,
    private val getTargetPage: GetTargetPageUseCase,
) : ViewModel(),
    StateHolder<ExamSessionUiState> by DefaultStateHolder(ExamSessionUiState()),
    EffectPublisher<ExamSessionEffect> by DefaultEffectPublisher() {

    private var activeSessionHandle: ExamSessionHandle? = null
    private val allQuestionResults = mutableListOf<ExamQuestionResult>()
    private var sessionStartTimeMs = 0L
    private var currentQuestionStartTimeMs = 0L
    private var timerJob: Job? = null
    private val examId = UUID.randomUUID().toString()

    private val _globalMistakes = MutableStateFlow<List<ExamMistake>>(emptyList())
    val globalMistakes = _globalMistakes.asStateFlow()

    init {
        updateState { copy(scope = scope, totalQuestions = questionCount) }
        prepareExam()
    }

    private suspend fun buildExpectedWordsForLines(startSurah: Int, startAyah: Int): Pair<List<WordFeedback>, Pair<Int, Int>> {
        val pageResult = getTargetPage.forAyah(startSurah, startAyah)
        var currentPageNum = (pageResult as? com.iti.domain.core.Result.Success)?.data ?: return emptyList<WordFeedback>() to (startSurah to startAyah)

        var linesCollected = 0
        var started = false
        val targetIndices = java.util.LinkedHashMap<Pair<Int, Int>, MutableList<Int>>()

        while (linesCollected < linesPerQuestion && currentPageNum <= 604) {
            val page = getPage(currentPageNum).first().getOrNull() ?: break
            for (line in page.lines) {
                if (line.type == com.example.mushaf.domain.model.LineType.SURAH_NAME) continue

                if (!started) {
                    val hasStart = line.words.any {
                        val cursor = RecitationCursor.fromWordId(it.id)
                        cursor != null && cursor.sura == startSurah && cursor.aya == startAyah
                    }
                    if (hasStart) started = true
                }

                if (started) {
                    linesCollected++
                    for (word in line.words) {
                        if (word.isEndOfAyah) continue
                        val cursor = RecitationCursor.fromWordId(word.id) ?: continue

                        if (cursor.sura > startSurah || (cursor.sura == startSurah && cursor.aya >= startAyah)) {
                            val key = cursor.sura to cursor.aya
                            targetIndices.getOrPut(key) { mutableListOf() }.add(cursor.wordIndex)
                        }
                    }
                    if (linesCollected >= linesPerQuestion) break
                }
            }
            if (linesCollected >= linesPerQuestion) break
            currentPageNum++
        }

        val expectedWordsList = mutableListOf<WordFeedback>()
        var finalSurah = startSurah
        var finalAyah = startAyah

        for ((key, indices) in targetIndices) {
            val (s, a) = key
            val result = mushafRepository.getAyahText(s, a)
            val text = if (result is com.iti.domain.core.Result.Success) result.data ?: "" else continue
            val ayahWords = text.split(Regex("\\s+")).filter { it.isNotBlank() }

            for (wIdx in indices) {
                val w = ayahWords.getOrNull(wIdx) ?: continue
                expectedWordsList.add(WordFeedback(uthmani = w, status = WordStatus.PENDING, surah = s, ayah = a, wordIdx = wIdx))
            }

            finalSurah = s
            finalAyah = a
        }

        return expectedWordsList to (finalSurah to finalAyah)
    }

    private fun prepareExam() = viewModelScope.launch {
        updateState { copy(isPreparing = true, hasConnectionError = false) }
        try {
            val rawQuestions = generateQuestions(scope, questionCount, linesPerQuestion)

            val questions = rawQuestions.map { q ->
                val (expectedWordsList, finalPos) = buildExpectedWordsForLines(q.surahNumber, q.ayahNumber)
                q.copy(endSurahNumber = finalPos.first, endAyahNumber = finalPos.second) to expectedWordsList
            }

            val initialQ = questions[0].first
            val initialExpected = questions[0].second

            updateState {
                copy(
                    isPreparing = false,
                    questions = questions.map { it.first },
                    currentQuestionIndex = 0,
                    activeQuestionResult = ExamQuestionResult(initialQ, words = initialExpected),
                    currentRecitationSurah = initialQ.surahNumber,
                    currentRecitationAyah = initialQ.ayahNumber,
                    currentRecitationWord = 0,
                    questionStatuses = emptyList(),
                )
            }
            sessionStartTimeMs = System.currentTimeMillis()
            startCountdown()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate exam questions", e)
            sendEffect(ExamSessionEffect.NavigateBack)
        }
    }

    private fun startCountdown() = viewModelScope.launch {
        updateState { copy(countdownSeconds = 3, revealedWordsCount = 0, showFullHint = false) }
        for (i in 3 downTo 1) {
            updateState { copy(countdownSeconds = i) }
            delay(1000)
        }
        updateState { copy(countdownSeconds = null) }
        startRecording()
    }

    fun onIntent(intent: ExamSessionIntent) {
        when (intent) {
            ExamSessionIntent.StartRecording -> startRecording()
            ExamSessionIntent.StopRecording -> stopRecording()
            ExamSessionIntent.SkipQuestion -> skipQuestion()
            ExamSessionIntent.EndSessionClicked -> updateState { copy(showEndConfirmDialog = true) }
            ExamSessionIntent.EndSessionConfirmDismissed -> updateState { copy(showEndConfirmDialog = false) }
            ExamSessionIntent.EndSessionConfirmed -> finishExam()
            ExamSessionIntent.ToggleTrackerVisibility -> updateState { copy(isTrackerVisible = !isTrackerVisible) }
            ExamSessionIntent.NextQuestionClicked -> {
                currentState.activeQuestionResult?.let { result ->
                    moveToNextQuestion(result)
                }
            }
            ExamSessionIntent.RevealNextWord -> {
                updateState { copy(revealedWordsCount = revealedWordsCount + 1) }
            }
            ExamSessionIntent.RevealFullHint -> {
                updateState { copy(showFullHint = true) }
            }
            ExamSessionIntent.ShowMistakesSheet -> {
                updateState { copy(showMistakesSheet = true) }
            }
            ExamSessionIntent.DismissMistakesSheet -> {
                updateState { copy(showMistakesSheet = false) }
            }
            is ExamSessionIntent.AudioPcmCaptured -> sendAudio(intent.pcm16)
        }
    }

    private fun startRecording() = viewModelScope.launch {
        val q = currentState.currentQuestion ?: return@launch
        updateState { copy(isRecording = true, isConnecting = true, hasConnectionError = false, elapsedSeconds = 0, showHasbuk = false, isReviewing = false) }
        currentQuestionStartTimeMs = System.currentTimeMillis()

        try {
            activeSessionHandle = dataSource.openSession(
                surah = q.surahNumber,
                ayah = q.ayahNumber,
                onFeedback = { handleFeedback(it) },
                onDone = { handleDone(it) },
                onError = { handleConnectionError() },
            )
            updateState { copy(isConnecting = false) }
            startTimer()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open session", e)
            handleConnectionError()
        }
    }

    private fun stopRecording() = viewModelScope.launch {
        timerJob?.cancel()
        updateState { copy(isRecording = false, isConnecting = false) }
        activeSessionHandle?.sendEnd()
    }

    private fun skipQuestion() = viewModelScope.launch {
        timerJob?.cancel()
        activeSessionHandle?.close()
        activeSessionHandle = null

        val q = currentState.currentQuestion ?: return@launch
        val result = currentState.activeQuestionResult?.copy(skipped = true)
            ?: ExamQuestionResult(q, skipped = true)

        moveToNextQuestion(result)
    }

    private fun sendAudio(pcm: ByteArray) = viewModelScope.launch {
        if (currentState.isRecording) {
            activeSessionHandle?.sendAudio(pcm)
        }
    }

    private fun handleFeedback(event: FeedbackEvent) = viewModelScope.launch {
        val q = currentState.currentQuestion ?: return@launch
        val currentResult = currentState.activeQuestionResult ?: ExamQuestionResult(q)

        updateState {
            copy(
                currentRecitationSurah = event.cursorSurah,
                currentRecitationAyah = event.cursorAyah,
                currentRecitationWord = event.cursorWord,
                lastFeedbackStatus = if (event.feedbackStatus == "progress") lastFeedbackStatus else event.feedbackStatus,
                candidates = if (event.candidates.isNotEmpty()) event.candidates else candidates,
            )
        }

        if (event.feedbackStatus != "ok") return@launch

        val updatedWords = currentResult.words.toMutableList()

        event.words.forEach { fw ->
            var absIdx = updatedWords.indexOfFirst { it.surah == fw.surah && it.ayah == fw.ayah && it.wordIdx == fw.wordIdx }

            if (absIdx == -1) {
                absIdx = updatedWords.indexOfFirst { it.surah == fw.surah && it.ayah == fw.ayah && it.wordIdx == (fw.wordIdx - 1) }
            }
            if (absIdx == -1) {
                absIdx = updatedWords.indexOfFirst { it.surah == fw.surah && it.ayah == fw.ayah && it.wordIdx == (fw.wordIdx + 1) }
            }
            if (absIdx == -1) {
                absIdx = updatedWords.indexOfFirst { it.surah == fw.surah && it.ayah == fw.ayah && it.status == WordStatus.PENDING }
            }

            if (absIdx != -1) {
                updatedWords[absIdx] = fw.copy(
                    wordIdx = updatedWords[absIdx].wordIdx,
                    uthmani = updatedWords[absIdx].uthmani
                )
            }
        }

        val accumulatedMistakes = updatedWords.filter { it.status == WordStatus.ERROR && !it.trimmed }.map { word ->
            ExamMistake(
                surahNumber = word.surah,
                ayahNumber = word.ayah,
                wordIndex = word.wordIdx,
                word = word.uthmani,
                category = classifyMistake(word.primaryErrorType ?: word.errorLabels.firstOrNull()),
                ruleName = word.errorLabels.firstOrNull(),
            )
        }

        val updatedResult = currentResult.copy(
            words = updatedWords,
            mistakes = accumulatedMistakes,
        )
        updateState { copy(activeQuestionResult = updatedResult) }

        _globalMistakes.value = allQuestionResults.flatMap { it.mistakes } + accumulatedMistakes

        val lastExpectedWord = updatedWords.lastOrNull()
        val hasPassedQuestion = lastExpectedWord != null && (
                (event.cursorSurah > lastExpectedWord.surah) ||
                        (event.cursorSurah == lastExpectedWord.surah && event.cursorAyah > lastExpectedWord.ayah) ||
                        (event.cursorSurah == lastExpectedWord.surah && event.cursorAyah == lastExpectedWord.ayah && event.cursorWord >= lastExpectedWord.wordIdx)
                )

        if (hasPassedQuestion && currentState.isRecording) {
            updateState { copy(showHasbuk = true) }
            sendEffect(ExamSessionEffect.PlayStopSound)
            stopRecording()
            delay(1500)
            moveToNextQuestion(updatedResult)
        }
    }

    private fun handleDone(event: FeedbackEvent) = viewModelScope.launch {
        if (event.words.isNotEmpty()) {
            handleFeedback(event)
        }

        val q = currentState.currentQuestion ?: return@launch
        val duration = System.currentTimeMillis() - currentQuestionStartTimeMs
        val finalResult = currentState.activeQuestionResult?.copy(durationMs = duration)
            ?: ExamQuestionResult(q, durationMs = duration)

        updateState {
            copy(
                activeQuestionResult = finalResult,
                isReviewing = true,
                isRecording = false,
                isConnecting = false,
            )
        }
        activeSessionHandle?.close()
        activeSessionHandle = null
        timerJob?.cancel()
    }

    private fun handleConnectionError() {
        timerJob?.cancel()
        updateState { copy(isRecording = false, isConnecting = false, hasConnectionError = true) }
        activeSessionHandle?.close()
        activeSessionHandle = null
    }

    private fun moveToNextQuestion(result: ExamQuestionResult) = viewModelScope.launch {
        allQuestionResults.add(result)
        _globalMistakes.value = allQuestionResults.flatMap { it.mistakes }

        activeSessionHandle?.close()
        activeSessionHandle = null
        timerJob?.cancel()

        val newStatuses = currentState.questionStatuses + result.questionStatus
        val nextIdx = currentState.currentQuestionIndex + 1

        if (nextIdx >= currentState.totalQuestions) {
            updateState { copy(questionStatuses = newStatuses) }
            finishExam()
        } else {
            val nextQ = currentState.questions[nextIdx]
            val (expectedWordsList, _) = buildExpectedWordsForLines(nextQ.surahNumber, nextQ.ayahNumber)

            updateState {
                copy(
                    currentQuestionIndex = nextIdx,
                    activeQuestionResult = ExamQuestionResult(nextQ, words = expectedWordsList),
                    currentRecitationSurah = nextQ.surahNumber,
                    currentRecitationAyah = nextQ.ayahNumber,
                    currentRecitationWord = 0,
                    isRecording = false,
                    isConnecting = false,
                    isReviewing = false,
                    showHasbuk = false,
                    hasConnectionError = false,
                    lastFeedbackStatus = "ok",
                    candidates = emptyList(),
                    elapsedSeconds = 0,
                    questionStatuses = newStatuses,
                    revealedWordsCount = 0,
                    showFullHint = false,
                    showMistakesSheet = false,
                )
            }
        }
    }

    private fun finishExam() = viewModelScope.launch {
        timerJob?.cancel()
        val currentQ = currentState.currentQuestion

        if (currentQ != null && allQuestionResults.size == currentState.currentQuestionIndex) {
            val wordsEvaluated = currentState.activeQuestionResult?.words?.any { it.status != WordStatus.PENDING } == true

            val finalRes = currentState.activeQuestionResult?.copy(
                durationMs = System.currentTimeMillis() - currentQuestionStartTimeMs,
                skipped = !wordsEvaluated
            ) ?: ExamQuestionResult(currentQ, skipped = true)
            allQuestionResults.add(finalRes)
            _globalMistakes.value = allQuestionResults.flatMap { it.mistakes }
        }

        activeSessionHandle?.close()
        activeSessionHandle = null
        updateState { copy(showEndConfirmDialog = false, isRecording = false, isConnecting = false) }

        val summary = ExamSummary(
            id = examId,
            scope = currentState.scope,
            startedAtMs = sessionStartTimeMs,
            totalDurationMs = System.currentTimeMillis() - sessionStartTimeMs,
            questionResults = allQuestionResults.toList(),
        )

        // 3. CACHE the summary BEFORE navigating!
        ExamSessionCache.latestSummary = summary

        saveSummary(summary)
        sendEffect(ExamSessionEffect.NavigateToSummary(summary.id))
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && currentState.isRecording) {
                delay(1_000)
                updateState { copy(elapsedSeconds = elapsedSeconds + 1) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        activeSessionHandle?.close()
    }
}