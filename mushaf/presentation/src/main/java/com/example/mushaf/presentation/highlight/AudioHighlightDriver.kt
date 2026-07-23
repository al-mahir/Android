package com.example.mushaf.presentation.highlight

import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.presentation.audio.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch




 
class AudioHighlightDriver(
    private val scope: CoroutineScope,
    private val playbackManager: AudioPlayer,
) : HighlightDriver {

    private val _currentWordId = MutableStateFlow<String?>(null)
    override val currentWordId: StateFlow<String?> = _currentWordId

    private var positionJob: Job? = null
    private var timingsList = emptyList<AyahTiming>()

    

 
    fun loadTimings(timings: List<AyahTiming>) {
        timingsList = timings
    }

    override fun start(page: MushafPage) {
        positionJob?.cancel()
        positionJob = scope.launch {
            kotlinx.coroutines.flow.combine(
                playbackManager.currentTrackIndex,
                playbackManager.currentPosition
            ) { trackIndex, currentMs ->
                if (trackIndex in timingsList.indices) {
                    val ayahTiming = timingsList[trackIndex]
                    updateHighlightForPosition(ayahTiming, currentMs)
                } else {
                    _currentWordId.value = null
                }
            }.collect {}
        }
    }

    override fun stop() {
        positionJob?.cancel()
        positionJob = null
        _currentWordId.value = null
    }

    private fun updateHighlightForPosition(ayahTiming: AyahTiming, positionMs: Long) {
        
        val wordTiming = ayahTiming.wordTimings.find { positionMs >= it.startMs && positionMs < it.endMs }
        
        if (wordTiming != null) {
            val wordIndex1Based = wordTiming.wordIndex + 1
            val wordId = "${ayahTiming.surahNumber}:${ayahTiming.ayahNumber}:$wordIndex1Based"
            if (_currentWordId.value != wordId) {
                android.util.Log.d("AudioHighlightDriver", "Highlighting word: $wordId at pos $positionMs")
                _currentWordId.value = wordId
            }
        } else {
            
            val lastWord = ayahTiming.wordTimings.lastOrNull()
            if (lastWord != null && positionMs >= lastWord.endMs) {
                _currentWordId.value = null
            }
        }
    }
}
