package com.example.mushaf.presentation.audio

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val audioState: StateFlow<AudioState>
    val currentPosition: StateFlow<Long>
    val currentTrackIndex: StateFlow<Int>
    val playbackSpeed: StateFlow<Float>

    fun playUrls(urls: List<String>)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun release()
}
