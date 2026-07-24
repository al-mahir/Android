package com.example.mushaf.presentation.state

import com.example.mushaf.domain.model.MushafMode
import com.example.mushaf.domain.model.recite.RecitationCursor

import com.example.mushaf.domain.model.Reciter

sealed interface MushafIntent {
    data class LoadPage(val page: Int) : MushafIntent

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

    // Surah Picker
    data object ShowSurahPicker : MushafIntent
    data object HideSurahPicker : MushafIntent
    data class NavigateToSurah(val surahNumber: Int) : MushafIntent

    // Tajweed Legend
    data object ShowTajweedLegend : MushafIntent
    data object HideTajweedLegend : MushafIntent

    data class SetTajweedGrading(val enabled: Boolean) : MushafIntent

    data class CaptureFailed(val error: CaptureError) : MushafIntent

    data object DismissCaptureError : MushafIntent

     
    data class SelectMistake(val wordId: String?) : MushafIntent

     
    data object DismissEngineNotice : MushafIntent
    data class SelectCandidate(val position: RecitationCursor) : MushafIntent

    data object DismissCandidates : MushafIntent

    data object DismissSessionSummary : MushafIntent

    data object FinishAndStartNewSession : MushafIntent
    data class SelectReciter(val reciter: Reciter) : MushafIntent
    data object PlayPauseAudio : MushafIntent
    data class SetAudioSpeed(val speed: Float) : MushafIntent
    data class SeekAudio(val positionMs: Long) : MushafIntent
    data object NextAyahAudio : MushafIntent
    data object PrevAyahAudio : MushafIntent

    // Tafsir
    data class LoadTafsir(val surah: Int, val ayah: Int) : MushafIntent
    data object DismissTafsir : MushafIntent

    // User Guide
    data object GuideNextStep : MushafIntent
    data object DismissGuide : MushafIntent
}
