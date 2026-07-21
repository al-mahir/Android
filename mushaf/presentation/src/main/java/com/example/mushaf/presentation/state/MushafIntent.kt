package com.example.mushaf.presentation.state

import com.example.mushaf.domain.model.MushafMode

import com.example.mushaf.domain.model.Reciter

sealed interface MushafIntent {
    data class LoadPage(val page: Int) : MushafIntent

    /**
     * Opens an explicitly requested page (e.g. Home's "Continue Reading"). Unlike [LoadPage]
     * this also suppresses the persisted-last-page restore, so a late preferences emission
     * cannot pull the reader back to where it previously was.
     */
    data class OpenAtPage(val page: Int) : MushafIntent
    data class ToggleTajweed(val enabled: Boolean) : MushafIntent
    data class HighlightWord(val wordId: String?) : MushafIntent
    data object StartFollowAlongPreview : MushafIntent
    data object StopFollowAlongPreview : MushafIntent
    data object Retry : MushafIntent
    data object ToggleBars : MushafIntent
    data class SetMode(val mode: MushafMode) : MushafIntent
    data object ToggleAyahVisibility : MushafIntent
    data object RevealNextWord : MushafIntent
    data object RevealNextAyah : MushafIntent
    data object ToggleRecording : MushafIntent

    // Listen Mode
    data class SelectReciter(val reciter: Reciter) : MushafIntent
    data object PlayPauseAudio : MushafIntent
    data class SetAudioSpeed(val speed: Float) : MushafIntent
    data class SeekAudio(val positionMs: Long) : MushafIntent
    data object NextAyahAudio : MushafIntent
    data object PrevAyahAudio : MushafIntent
}
