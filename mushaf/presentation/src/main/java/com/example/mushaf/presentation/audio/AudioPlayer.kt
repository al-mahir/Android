package com.example.mushaf.presentation.audio

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val audioState: StateFlow<AudioState>
    val currentPosition: StateFlow<Long>
    val currentTrackIndex: StateFlow<Int>
    val playbackSpeed: StateFlow<Float>
    val externalCommands: kotlinx.coroutines.flow.SharedFlow<String>

    data class AudioTrackInfo(
        val url: String,
        val title: String,
        val artist: String
    )

    fun playTracks(tracks: List<AudioTrackInfo>, startIndex: Int = 0)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun release()
}
