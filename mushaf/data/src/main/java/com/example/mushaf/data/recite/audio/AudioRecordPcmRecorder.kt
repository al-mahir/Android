package com.example.mushaf.data.recite.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import kotlinx.coroutines.Dispatchers
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
            val buffer = ShortArray(RecitationAudioFormat.FRAME_SAMPLES)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = recorder.read(buffer, 0, buffer.size)
                when {
                    read > 0 -> {
                        
                        
                        emit(AudioFrame(samples = buffer.copyOf(read), startSample = startSample))
                        startSample += read
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
    }.flowOn(Dispatchers.IO)

    


 
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
        const val FRAME_BUFFER_MULTIPLE = 4
    }
}
