package com.example.mushaf.data.repository

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.mushaf.data.MushafLog
import com.example.mushaf.data.recite.RecitationFeedbackMapper
import com.example.mushaf.data.recite.remote.LiveRecitationSocket
import com.example.mushaf.data.recite.remote.LiveSessionCommand
import com.example.mushaf.data.recite.remote.LiveSessionEvent
import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.RecitationControl
import com.example.mushaf.domain.model.recite.SpeechEvent
import com.example.mushaf.domain.repository.LiveRecitationRepository
import com.example.mushaf.domain.repository.RecitationCaptureRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Assembles the live correction pipeline: microphone → speech gate → session socket → domain.
 *
 * The three stages live behind one capability because they are inseparable in practice — a
 * session with a socket but no microphone, or audio with nowhere to send it, is not a partially
 * working feature, it is a broken one.
 */
class LiveRecitationRepositoryImpl(
    private val capture: RecitationCaptureRepository,
    private val socket: LiveRecitationSocket,
) : LiveRecitationRepository {

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun session(
        config: LiveRecitationConfig,
        controls: Flow<RecitationControl>,
    ): Flow<LiveRecitationEvent> = channelFlow {
        // Buffered so a slow network cannot stall the microphone read loop; the service does not
        // block sends, and dropping audio would be worse than queueing it.
        val commands = Channel<LiveSessionCommand>(Channel.BUFFERED)

        // Assigned once the handshake succeeds. The microphone deliberately stays shut until
        // then: a session that never starts should never have opened it, and the recording
        // indicator must not light for a connection that is about to fail.
        var audioJob: Job? = null

        // The reciter can tap stop before the handshake lands — on a slow first connection that
        // is a real sequence, not a theoretical one. Without this the late handshake would open
        // the microphone and stream into an already-closed command channel.
        val finishing = AtomicBoolean(false)

        val controlJob = launch {
            controls.collect { control ->
                when (control) {
                    is RecitationControl.Seek -> commands.send(
                        LiveSessionCommand.Seek(
                            sura = control.position.sura,
                            aya = control.position.aya,
                            wordIdx = control.position.wordIndex,
                        ),
                    )

                    RecitationControl.Finish -> {
                        finishing.set(true)
                        // Release the microphone *before* saying end, so no audio can arrive
                        // after the flush request and be silently discarded by the server.
                        audioJob?.cancelAndJoin()
                        commands.send(LiveSessionCommand.End)
                        commands.close()
                    }
                }
            }
        }

        try {
            socket.open(RecitationFeedbackMapper.toStartMessage(config), commands.receiveAsFlow())
                .collect { event ->
                    send(event.toDomain())
                    if (event is LiveSessionEvent.Started && audioJob == null && !finishing.get()) {
                        audioJob = launch { streamCaptureInto(commands, config) }
                    }
                }
        } finally {
            audioJob?.cancel()
            controlJob.cancel()
            commands.close()
        }
    }

    /**
     * Pumps gated microphone audio into [commands], reporting loudness as it goes.
     *
     * The level events are the only evidence the pipeline is alive during a silent stretch:
     * a session where nothing is said correctly produces no graded chunks at all.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private suspend fun ProducerScope<LiveRecitationEvent>.streamCaptureInto(
        commands: SendChannel<LiveSessionCommand>,
        config: LiveRecitationConfig,
    ) {
        capture.captureSpeech(config.speechGate).collect { event ->
            when (event) {
                is SpeechEvent.Audio -> {
                    commands.send(LiveSessionCommand.Audio(event.frame))
                    send(LiveRecitationEvent.Level(event.frame.rms(), isSpeaking = true))
                }

                SpeechEvent.SpeechEnded ->
                    send(LiveRecitationEvent.Level(amplitude = 0f, isSpeaking = false))
            }
        }
    }

    private fun LiveSessionEvent.toDomain(): LiveRecitationEvent = when (this) {
        is LiveSessionEvent.Started -> LiveRecitationEvent.Started(
            sessionId = sessionId,
            engine = engine,
            requestedEngine = requestedEngine,
        ).also {
            if (it.engineSubstituted) {
                Log.w(TAG, "Requested engine '$requestedEngine' but the server ran '$engine'")
            }
        }

        is LiveSessionEvent.Feedback ->
            LiveRecitationEvent.Graded(RecitationFeedbackMapper.toChunk(envelope))

        LiveSessionEvent.Done -> LiveRecitationEvent.Finished
    }

    private companion object {
        const val TAG = MushafLog.TAG
    }
}
