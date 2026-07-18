package com.example.mushaf.presentation.state

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
}
