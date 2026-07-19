package com.example.mushaf.presentation.state

import com.example.mushaf.domain.model.MushafMode

sealed interface MushafIntent {
    data class LoadPage(val page: Int) : MushafIntent
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
}
