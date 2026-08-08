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
import com.example.mushaf.domain.repository.AsrModelRepository
import com.example.mushaf.domain.repository.LiveRecitationRepository
import com.example.mushaf.domain.repository.LocalSpeechRecognizer
import com.example.mushaf.domain.repository.RecitationCaptureRepository
import com.iti.domain.core.Result
import com.iti.domain.core.asResult
import com.iti.domain.core.getOrNull
import kotlinx.coroutines.CoroutineStart
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
        var audioJob: Job? = null
        var localWordsJob: Job? = null
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
                        localWordsJob?.cancelAndJoin()
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
                        localWordsJob = launch(start = CoroutineStart.UNDISPATCHED) { streamLocalWordsInto() }
                        audioJob = launch { streamCaptureInto(commands, config) }
                    }
                }
        } finally {
            audioJob?.cancel()
            localWordsJob?.cancel()
            controlJob.cancel()
            commands.close()
        }
    }.asResult()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private suspend fun ProducerScope<LiveRecitationEvent>.streamCaptureInto(
        commands: SendChannel<LiveSessionCommand>,
        config: LiveRecitationConfig,
    ) {
        capture.captureSpeech(config.speechGate).collect { result ->
            val event = result.getOrNull() ?: return@collect
            when (event) {
                is SpeechEvent.Audio -> {
                    commands.send(LiveSessionCommand.Audio(event.frame))
                    send(LiveRecitationEvent.Level(event.frame.rms(), isSpeaking = event.isSpeech))
                    runCatching { localSpeechRecognizer.accept(event.frame) }
                        .onFailure { Log.w(TAG, "Local speech recognizer failed to accept a frame", it) }
                }

                SpeechEvent.SpeechEnded -> send(LiveRecitationEvent.Level(amplitude = 0f, isSpeaking = false))
            }
        }
    }

    private suspend fun ProducerScope<LiveRecitationEvent>.streamLocalWordsInto() {
        localSpeechRecognizer.words.collect { word -> send(LiveRecitationEvent.LocalWord(word)) }
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
