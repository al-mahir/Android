package com.example.mushaf.presentation.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AudioState {
    IDLE, BUFFERING, PLAYING, PAUSED, ERROR, ENDED
}



 
class AudioPlaybackManager(
    private val context: Context,
) : AudioPlayer {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _player: ExoPlayer = ExoPlayer.Builder(context).build()
    
    private val _audioState = MutableStateFlow(AudioState.IDLE)
    override val audioState: StateFlow<AudioState> = _audioState

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition

    private val _currentTrackIndex = MutableStateFlow(0)
    override val currentTrackIndex: StateFlow<Int> = _currentTrackIndex
    
    private val _playbackSpeed = MutableStateFlow(1f)
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private var positionJob: Job? = null

    init {
        _player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentTrackIndex.value = _player.currentMediaItemIndex
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateState()
                if (isPlaying) {
                    startPollingPosition()
                } else {
                    stopPollingPosition()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("AudioPlaybackManager", "Player error occurred: ${error.message}", error)
                _audioState.value = AudioState.ERROR
            }
        })
    }

    private fun updateState() {
        _audioState.value = when (_player.playbackState) {
            Player.STATE_IDLE -> AudioState.IDLE
            Player.STATE_BUFFERING -> AudioState.BUFFERING
            Player.STATE_READY -> if (_player.isPlaying) AudioState.PLAYING else AudioState.PAUSED
            Player.STATE_ENDED -> AudioState.ENDED
            else -> AudioState.IDLE
        }
    }

    private fun startPollingPosition() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                _currentPosition.value = _player.currentPosition
                delay(50L) 
            }
        }
    }

    private fun stopPollingPosition() {
        positionJob?.cancel()
        positionJob = null
        
        _currentPosition.value = _player.currentPosition
    }

    

 
    override fun playUrls(urls: List<String>) {
        _player.stop()
        _player.clearMediaItems()
        
        val mediaItems = urls.map { MediaItem.fromUri(it) }
        _player.addMediaItems(mediaItems)
        _player.prepare()
        _player.play()
    }

    override fun play() {
        _player.play()
    }

    override fun pause() {
        _player.pause()
    }

    override fun stop() {
        _player.stop()
        _player.clearMediaItems()
        _currentPosition.value = 0L
    }

    override fun seekTo(positionMs: Long) {
        _player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    override fun setSpeed(speed: Float) {
        _player.setPlaybackSpeed(speed)
        _playbackSpeed.value = speed
    }

    override fun release() {
        stopPollingPosition()
        _player.release()
        scope.cancel()
    }
}
