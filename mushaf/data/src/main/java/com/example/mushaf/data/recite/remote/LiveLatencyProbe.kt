package com.example.mushaf.data.recite.remote

import android.os.SystemClock
import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat

/**
 * Attributes live-correction latency to the client or the server, so "the feedback is slow" stops
 * being a guess.
 *
 * Every graded chunk carries `audio_span_sec`, the server's own timestamps over the audio it has
 * received. That is the same timeline as the bytes we push into the socket: the server's clock
 * counts *sent* audio, which since the speech gate stopped withholding silence is the same thing as
 * captured audio. Recording when each millisecond of that timeline left the device turns the span
 * end into an exact answer to "how long did the network and the server take with this audio".
 *
 * Two numbers come out of it, and they point at different owners:
 *
 * - **pipeline** - mic-to-socket. How long a frame sat in the app between the microphone finishing
 *   it and this client handing it to the WebSocket. Anything above ~150ms is a client problem:
 *   a blocked dispatcher, a stalled collector, or work on the audio path. It is measured against
 *   [AudioFrame.startMs], which counts from the moment capture started, so it needs no cooperation
 *   from the server.
 * - **server** - socket-to-feedback. Wall time from sending the last audio of a chunk to that
 *   chunk's feedback arriving: network round trip plus the server's own chunking and inference.
 *   Nothing on the device can shorten it.
 *
 * Filter for it with `adb logcat -s Mushaf | grep LATENCY`.
 */
internal class LiveLatencyProbe {

    private class Mark(val sentAudioMs: Long, val atElapsedMs: Long)

    /** Sent-audio position (ms) ↔ wall clock, oldest first. Bounded by [MAX_MARKS]. */
    private val marks = ArrayDeque<Mark>()

    /** Cumulative milliseconds of audio handed to the socket - the server's own timeline. */
    private var sentAudioMs = 0L

    /**
     * Wall clock the capture is estimated to have started at, derived from the frames' own
     * positions. Compared against instead of a "session started" stamp, which would fold
     * connection setup into what is meant to measure the audio path alone.
     *
     * Estimated as the *minimum* of `now - frameEnd` rather than taken from the first frame. For
     * any frame that estimate is `trueEpoch + itsLag`, and lag is never negative, so the smallest
     * one seen is the best estimate available and the number self-corrects downward. Anchoring on
     * the first frame was safe only while capture began after the socket ack; it now begins
     * alongside the handshake, so the first frames are deliberately sent late in a burst and would
     * otherwise fix the epoch a full handshake too late and under-report every lag after it.
     */
    private var captureEpochMs = UNSET

    private var worstPipelineLagMs = 0L
    private var lastSummaryAtMs = 0L

    fun onAudioSent(frame: AudioFrame) {
        val now = SystemClock.elapsedRealtime()
        val frameEndMs = frame.startMs + frame.durationMs

        if (captureEpochMs == UNSET) {
            lastSummaryAtMs = now
        }
        val epochEstimate = now - frameEndMs
        if (captureEpochMs == UNSET || epochEstimate < captureEpochMs) {
            captureEpochMs = epochEstimate
        }

        val pipelineLagMs = now - (captureEpochMs + frameEndMs)
        if (pipelineLagMs > worstPipelineLagMs) worstPipelineLagMs = pipelineLagMs

        sentAudioMs += frame.durationMs
        marks.addLast(Mark(sentAudioMs, now))
        while (marks.size > MAX_MARKS) marks.removeFirst()

        // A stall shows up as the gap between when the mic finished a frame and when it reached the
        // wire, and it only ever grows while the audio path is blocked - so report it as it opens
        // up rather than waiting for a chunk that may itself be late because of it.
        if (pipelineLagMs >= PIPELINE_LAG_WARN_MS && now - lastSummaryAtMs >= SUMMARY_INTERVAL_MS) {
            lastSummaryAtMs = now
            Log.w(
                TAG,
                "LATENCY client pipeline is behind: this frame reached the socket ${pipelineLagMs}ms " +
                    "after the microphone finished it (worst ${worstPipelineLagMs}ms). The audio " +
                    "path is being starved - the server cannot grade audio it has not received yet.",
            )
        }
    }

    /**
     * Logs the breakdown for one graded chunk. [spanEndSec] is `audio_span_sec`'s last entry - the
     * end of the audio this feedback covers, on the sent-audio timeline.
     */
    fun onFeedback(chunkSeq: Int, spanEndSec: Double?, forcedCut: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val spanEndMs = spanEndSec?.let { (it * 1000).toLong() }
        val sentAt = spanEndMs?.let { end -> marks.firstOrNull { it.sentAudioMs >= end }?.atElapsedMs }

        // Without a usable span (or with one older than the mark window) the split is unknowable;
        // saying so beats printing a number that silently means something else.
        val serverPart = when {
            sentAt != null -> "server ${now - sentAt}ms"
            spanEndMs == null -> "server unknown (no audio_span_sec in this chunk)"
            else -> "server unknown (chunk covers audio older than the ${MAX_MARKS / FRAMES_PER_SECOND}s mark window)"
        }

        val pipelineLagMs = if (captureEpochMs == UNSET) 0L else worstPipelineLagMs

        Log.i(
            TAG,
            "LATENCY chunk#$chunkSeq: $serverPart | client pipeline worst ${pipelineLagMs}ms | " +
                "audio sent ${sentAudioMs}ms" + if (forcedCut) " | FORCED_CUT" else "",
        )
        worstPipelineLagMs = 0
    }

    private companion object {
        const val TAG = MushafLog.TAG
        const val UNSET = -1L

        val FRAMES_PER_SECOND = RecitationAudioFormat.framesFor(1_000)

        /** ~60s of audio, whatever a frame currently is: longer than any chunk the server sends. */
        val MAX_MARKS = RecitationAudioFormat.framesFor(60_000)

        /**
         * A frame is expected on the wire within roughly one frame of the microphone finishing it.
         * 250ms is past any plausible scheduling jitter and squarely into "something is blocking
         * the audio path".
         */
        const val PIPELINE_LAG_WARN_MS = 250L

        /** Keeps a sustained stall to one line a second instead of ten. */
        const val SUMMARY_INTERVAL_MS = 1_000L
    }
}
