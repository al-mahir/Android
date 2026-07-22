package com.example.mushaf.data.repository

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.mushaf.data.MushafLog
import com.example.mushaf.data.recite.audio.PcmRecorder
import com.example.mushaf.data.recite.audio.WavDebugSink
import com.example.mushaf.data.recite.audio.WavFileWriter
import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.RecitationAudioFormat
import com.example.mushaf.domain.model.recite.SpeechEvent
import com.example.mushaf.domain.model.recite.SpeechGate
import com.example.mushaf.domain.model.recite.SpeechGateConfig
import com.example.mushaf.domain.repository.RecitationCaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow







 
class RecitationCaptureRepositoryImpl(
    private val recorder: PcmRecorder,
    private val debugSink: WavDebugSink,
) : RecitationCaptureRepository {

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun capture(): Flow<AudioFrame> = flow {
        val writer = debugSink.open(label = "raw")
        try {
            recorder.record().collect { frame ->
                writer.writeQuietly(frame)
                emit(frame)
            }
        } finally {
            writer.closeQuietly()
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun captureSpeech(config: SpeechGateConfig): Flow<SpeechEvent> = flow {
        val gate = SpeechGate(config)
        val rawWriter = debugSink.open(label = "raw")
        val gatedWriter = debugSink.open(label = "gated")

        try {
            recorder.record().collect { frame ->
                rawWriter.writeQuietly(frame)

                val wasOpen = gate.isOpen
                gate.process(frame).forEach { passed ->
                    gatedWriter.writeQuietly(passed)
                    emit(SpeechEvent.Audio(passed))
                }
                if (wasOpen && !gate.isOpen) {
                    emit(SpeechEvent.SpeechEnded)
                }
            }
        } finally {
            logSavings(gate, config)
            rawWriter.closeQuietly()
            gatedWriter.closeQuietly()
        }
    }

    


 
    private fun logSavings(gate: SpeechGate, config: SpeechGateConfig) {
        val stats = gate.stats
        if (stats.framesIn == 0L) return
        val frameMs = RecitationAudioFormat.FRAME_DURATION_MS
        Log.i(
            MushafLog.TAG,
            "Speech gate (enabled=${config.enabled}): sent ${stats.framesOut}/${stats.framesIn} " +
                "frames (${stats.framesOut * frameMs}ms of ${stats.framesIn * frameMs}ms), " +
                "dropped ${"%.1f".format(stats.droppedFraction * 100)}%, " +
                "noise floor ${"%.4f".format(gate.noiseFloorEstimate)}",
        )
    }

    
    private fun WavFileWriter?.writeQuietly(frame: AudioFrame) {
        this ?: return
        runCatching { write(frame) }
            .onFailure { Log.w(MushafLog.TAG, "Debug capture write failed", it) }
    }

    private fun WavFileWriter?.closeQuietly() {
        this ?: return
        runCatching { close() }
            .onFailure { Log.w(MushafLog.TAG, "Debug capture close failed", it) }
    }
}
