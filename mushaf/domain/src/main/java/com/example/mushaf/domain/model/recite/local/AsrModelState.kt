package com.example.mushaf.domain.model.recite.local

/**
 * Availability of the bundled-on-first-use local ASR model (see [com.example.mushaf.domain.repository.AsrModelRepository]).
 * Deliberately separate from [com.example.mushaf.domain.model.DownloadState]/`ResourceKind` -
 * this isn't a user-browsed catalogue item like a reciter or a tafsir book, it's an internal
 * dependency the local-word tracking path needs and fetches transparently the first time it's
 * used. Never blocks or fails a live-correction session either way: while anything other than
 * [Ready], local word tracking simply doesn't run for that session, and the server-graded cursor
 * remains the sole source of truth (same as today), no different from the model being
 * unavailable for any other reason.
 */
sealed interface AsrModelState {
    data object NotDownloaded : AsrModelState
    data class Downloading(val progress: Float) : AsrModelState
    data object Ready : AsrModelState
    data class Failed(val message: String?) : AsrModelState
}
