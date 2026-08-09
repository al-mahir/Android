package com.iti.domain.repository.exam

import com.iti.domain.model.exam.WordFeedback
import com.iti.domain.model.exam.WordStatus

/** A candidate match returned when the recited passage is ambiguous. */
data class FeedbackCandidate(
    val surah: Int,
    val ayah: Int,
    val uthmaniText: String,
)

/**
 * Parsed AI feedback event delivered to the ViewModel.
 *
 * [feedbackStatus]: "ok" | "ambiguous" | "no_match" | "progress" — whether the passage was identified.
 * [words]: per-word grading, only meaningful when [feedbackStatus] == "ok".
 * [candidates]: populated only when [feedbackStatus] == "ambiguous".
 * [nonVerse]: recognised non-verse text (istiaatha / basmalah / sadaka) that was excluded.
 * [confirmedProgressWords]: The number of words the user has currently spoken in real-time.
 */
data class FeedbackEvent(
    val words: List<WordFeedback>,
    val uthmaniText: String? = null,
    /** "ok" | "ambiguous" | "no_match" | "done" | "progress" */
    val feedbackStatus: String = "ok",
    val cursorSurah: Int,
    val cursorAyah: Int,
    val cursorWord: Int,
    val isDone: Boolean = false,
    val candidates: List<FeedbackCandidate> = emptyList(),
    val nonVerse: List<String> = emptyList(),
    val confirmedProgressWords: Int = 0, // FIXED: Added this property to support real-time text syncing
)

/**
 * Handle to an open WebSocket exam session.
 * The caller must [close] it when the question ends (or on error).
 */
interface ExamSessionHandle {
    /** Send raw PCM-16 audio bytes (16 kHz, mono) captured from the microphone. */
    suspend fun sendAudio(pcm16Bytes: ByteArray)
    /** Tell the server the recitation has ended; triggers the final feedback + "done" event. */
    suspend fun sendEnd()
    /**
     * Seek to a new position (e.g., user tapped a different ayah).
     * No server reply is sent; the cursor resets immediately.
     */
    suspend fun seek(surah: Int, ayah: Int, wordIdx: Int = 0)
    /** Close the WebSocket immediately without waiting for a "done" response. */
    fun close()
}

/**
 * Abstraction over the AI WebSocket recitation scoring service.
 *
 * One [ExamSessionHandle] corresponds to one question (one WebSocket connection).
 * The caller drives audio streaming via [ExamSessionHandle.sendAudio]; the implementation
 * delivers incoming [FeedbackEvent]s via [onFeedback] and [onDone] callbacks.
 */
interface ExamDataSource {

    /**
     * Opens a new WebSocket session for one exam question.
     *
     * @param surah     1-based sūrah number of the question ayah.
     * @param ayah      1-based ayah number of the question ayah.
     * @param strictness AI scoring strictness ("normal" | "strict" | "lenient").
     * @param onFeedback Called with each feedback frame received from the server.
     * @param onDone     Called when the server sends its final "done" event.
     * @param onError    Called if the WebSocket connection fails or an exception is thrown.
     * @return           A [ExamSessionHandle] for sending audio and ending the session.
     */
    suspend fun openSession(
        surah: Int,
        ayah: Int,
        strictness: String = "normal",
        onFeedback: suspend (FeedbackEvent) -> Unit,
        onDone: suspend (FeedbackEvent) -> Unit,
        onError: suspend (Throwable) -> Unit,
    ): ExamSessionHandle
}