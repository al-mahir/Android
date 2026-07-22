package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.RecitationControl
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.repository.LiveRecitationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Starts a live correction session for a passage.
 *
 * Owns the one product rule that must not be re-decided per screen: **a session always carries
 * the reciter's position when the caller knows it**. Without a cursor the service falls back to
 * a whole-muṣḥaf fuzzy search, which returns `ambiguous` for the basmalah — the single most
 * likely thing a reciter opens with.
 */
class StartLiveRecitationUseCase(
    private val repository: LiveRecitationRepository,
) {
    operator fun invoke(
        config: LiveRecitationConfig,
        controls: Flow<RecitationControl>,
    ): Flow<LiveRecitationEvent> = repository.session(config, controls)

    /** Convenience for the common case: a reading view that always knows where the reciter is. */
    operator fun invoke(
        from: RecitationCursor,
        controls: Flow<RecitationControl>,
    ): Flow<LiveRecitationEvent> = repository.session(LiveRecitationConfig(start = from), controls)
}
