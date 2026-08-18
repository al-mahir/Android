package com.example.mushaf.data.recite.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn








 
class AudioRecordPcmRecorder : PcmRecorder {

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun record(): Flow<AudioFrame> = flow {
        val bufferBytes = resolveBufferSizeBytes()
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            RecitationAudioFormat.SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferBytes,
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            error("AudioRecord failed to initialise at ${RecitationAudioFormat.SAMPLE_RATE_HZ} Hz mono PCM16")
        }

        try {
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                error("AudioRecord did not start; the microphone is held by another app")
            }
            Log.d(TAG, "Capture started: 16kHz mono PCM16, buffer=$bufferBytes bytes, source=VOICE_RECOGNITION")

            var startSample = 0L
            val startedAtMs = SystemClock.elapsedRealtime()
            var lastDriftLogAtMs = startedAtMs
            val buffer = ShortArray(RecitationAudioFormat.FRAME_SAMPLES)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = recorder.read(buffer, 0, buffer.size)
                when {
                    read > 0 -> {


                        emit(AudioFrame(samples = buffer.copyOf(read), startSample = startSample))
                        startSample += read
                        lastDriftLogAtMs = warnIfDrifting(startSample, startedAtMs, lastDriftLogAtMs)
                    }
                    read == 0 -> Unit
                    else -> error("AudioRecord.read failed with ${readErrorName(read)}")
                }
            }
        } finally {
            runCatching { recorder.stop() }
                .onFailure { Log.w(TAG, "AudioRecord.stop() failed; releasing anyway", it) }
            recorder.release()
            Log.d(TAG, "Capture stopped, microphone released")
        }
    }.flowOn(AudioCaptureDispatcher.instance)

    



    private fun warnIfDrifting(capturedSamples: Long, startedAtMs: Long, lastLogAtMs: Long): Long {
        val now = SystemClock.elapsedRealtime()
        val driftMs = (now - startedAtMs) - RecitationAudioFormat.durationMsOf(capturedSamples)
        if (driftMs < CAPTURE_DRIFT_WARN_MS || now - lastLogAtMs < DRIFT_LOG_INTERVAL_MS) return lastLogAtMs
        Log.w(
            TAG,
            "LATENCY capture is ${driftMs}ms behind real time - frames are being read late because " +
                "the consumer is slow. Everything downstream, grading included, is that far stale.",
        )
        return now
    }

    private fun resolveBufferSizeBytes(): Int {
        val minimum = AudioRecord.getMinBufferSize(
            RecitationAudioFormat.SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minimum != AudioRecord.ERROR && minimum != AudioRecord.ERROR_BAD_VALUE) {
            "Device does not support 16 kHz mono PCM16 capture (getMinBufferSize=$minimum)"
        }
        return maxOf(minimum, RecitationAudioFormat.FRAME_BYTES * FRAME_BUFFER_MULTIPLE)
    }

    private fun readErrorName(code: Int): String = when (code) {
        AudioRecord.ERROR_INVALID_OPERATION -> "ERROR_INVALID_OPERATION"
        AudioRecord.ERROR_BAD_VALUE -> "ERROR_BAD_VALUE"
        AudioRecord.ERROR_DEAD_OBJECT -> "ERROR_DEAD_OBJECT"
        else -> "ERROR($code)"
    }

    private companion object {
        const val TAG = MushafLog.TAG

        /**
         * Frames of slack in `AudioRecord`'s own ring buffer, expressed in frames so that
         * shrinking [RecitationAudioFormat.FRAME_DURATION_MS] shrinks the *latency* of a read
         * without also shrinking the headroom that keeps a scheduling hiccup from overrunning the
         * buffer. Deliberately kept at ~800ms, which is what it was when a frame was 100ms.
         */
        val FRAME_BUFFER_MULTIPLE = RecitationAudioFormat.framesFor(800)

        const val CAPTURE_DRIFT_WARN_MS = 150L
        const val DRIFT_LOG_INTERVAL_MS = 1_000L
    }
}
