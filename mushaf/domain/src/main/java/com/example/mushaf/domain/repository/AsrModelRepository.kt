package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.local.AsrModelState
import kotlinx.coroutines.flow.StateFlow

/**
 * Fetches and tracks the on-device streaming ASR model used for local word/cursor tracking (see
 * docs/features/06-taahud-speechrecognizer-status.md for the architecture history this replaces).
 * Downloaded transparently on first use rather than bundled in the APK - see [AsrModelState]'s
 * doc for why this isn't part of the user-facing Downloads catalogue.
 */
interface AsrModelRepository {

    val state: StateFlow<AsrModelState>

    /**
     * Starts the download if not already downloaded/downloading; safe to call every time a live
     * session starts. Never throws - failures land in [AsrModelState.Failed] via [state].
     */
    fun ensureAvailable()
}
