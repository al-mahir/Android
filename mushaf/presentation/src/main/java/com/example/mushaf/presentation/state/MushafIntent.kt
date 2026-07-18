package com.example.mushaf.presentation.state

sealed interface MushafIntent {
    data class LoadPage(val page: Int) : MushafIntent
    data class ToggleTajweed(val enabled: Boolean) : MushafIntent
    data class HighlightWord(val wordId: String?) : MushafIntent
    data object StartFollowAlongPreview : MushafIntent
    data object StopFollowAlongPreview : MushafIntent
    data object Retry : MushafIntent
}
