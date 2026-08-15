package com.example.mushaf.data.repository

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.mushaf.data.MushafLog
import com.example.mushaf.data.recite.RecitationFeedbackMapper
import com.example.mushaf.data.recite.remote.LiveRecitationSocket
import com.example.mushaf.data.recite.remote.LiveSessionCommand
import com.example.mushaf.data.recite.remote.LiveSessionEvent
import com.example.mushaf.domain.model.recite.AudioFrame
import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.RecitationControl
import com.example.mushaf.domain.model.recite.SpeechEvent
import com.example.mushaf.domain.repository.AsrModelRepository
import com.example.mushaf.domain.repository.LiveRecitationRepository
import com.example.mushaf.domain.repository.LocalSpeechRecognizer
import com.example.mushaf.domain.repository.RecitationCaptureRepository
import com.iti.domain.core.Result
import com.iti.domain.core.asResult
import com.iti.domain.core.getOrNull
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class LiveRecitationRepositoryImpl(
    private val capture: RecitationCaptureRepository,
    private val socket: LiveRecitationSocket,
    private val localSpeechRecognizer: LocalSpeechRecognizer,
    private val asrModelRepository: AsrModelRepository,
) : LiveRecitationRepository {

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun session(
        config: LiveRecitationConfig,
        controls: Flow<RecitationControl>,
    ): Flow<Result<LiveRecitationEvent>> = channelFlow {
        asrModelRepository.ensureAvailable()
        localSpeechRecognizer.reset()

        val commands = Channel<LiveSessionCommand>(Channel.BUFFERED)
        // Frames destined for on-device inference go through their own buffer, and the *oldest*
        // are dropped when it fills. The local recognizer used to be called inline from the
        // capture loop, which put its decode latency directly in front of the next
        // `commands.send()` - i.e. in front of the grading audio the whole feature depends on.
        // Local tracking is advisory: losing a stretch of it under load is a far better outcome
        // than delaying the server's audio, and dropping the oldest keeps the decoder near
        // real time instead of falling further behind.
        val localFrames = Channel<AudioFrame>(
            capacity = LOCAL_FRAME_BUFFER,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        var audioJob: Job? = null
        var localTranscriptJob: Job? = null
        var localDecodeJob: Job? = null
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
                        audioJob?.cancelAndJoin()
                        localDecodeJob?.cancelAndJoin()
                        localTranscriptJob?.cancelAndJoin()
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
                        localTranscriptJob = launch(start = CoroutineStart.UNDISPATCHED) { streamLocalTranscriptInto() }
                        localDecodeJob = launch { decodeLocally(localFrames) }
                        audioJob = launch { streamCaptureInto(commands, localFrames, config) }
                    }
                }
        } finally {
            audioJob?.cancel()
            localDecodeJob?.cancel()
            localTranscriptJob?.cancel()
            controlJob.cancel()
            localFrames.close()
            commands.close()
        }
    }
              .flowOn(Dispatchers.IO)
        .asResult()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private suspend fun ProducerScope<LiveRecitationEvent>.streamCaptureInto(
        commands: SendChannel<LiveSessionCommand>,
        localFrames: SendChannel<AudioFrame>,
        config: LiveRecitationConfig,
    ) {
        capture.captureSpeech(config.speechGate).collect { result ->
            val event = result.getOrNull() ?: return@collect
            when (event) {
                is SpeechEvent.Audio -> {
                    commands.send(LiveSessionCommand.Audio(event.frame))
                         trySend(LiveRecitationEvent.Level(event.frame.rms(), isSpeaking = event.isSpeech))
                    localFrames.trySend(event.frame)
                }

                  SpeechEvent.SpeechEnded -> send(LiveRecitationEvent.Level(amplitude = 0f, isSpeaking = false))
            }
        }
    }


    private suspend fun decodeLocally(localFrames: ReceiveChannel<AudioFrame>) {
        for (frame in localFrames) {
            runCatching { localSpeechRecognizer.accept(frame) }
                .onFailure { Log.w(TAG, "Local speech recognizer failed to accept a frame", it) }
        }
    }


    private suspend fun ProducerScope<LiveRecitationEvent>.streamLocalTranscriptInto() {
        localSpeechRecognizer.transcript.collect { trySend(LiveRecitationEvent.LocalPhonemes(it)) }
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

              const val LOCAL_FRAME_BUFFER = 32
    }
}
