package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.LiveRecitationConfig
import com.example.mushaf.domain.model.recite.LiveRecitationEvent
import com.example.mushaf.domain.model.recite.RecitationControl
import kotlinx.coroutines.flow.Flow

/**
 * Live AI correction of a recitation.
 *
 * One capability, because a session is one indivisible thing: the microphone, the speech gate
 * and the service connection start and stop together, and a caller that could start one without
 * the others would only be able to build a broken session.
 */
interface LiveRecitationRepository {

    /**
     * Runs a session until [controls] emits [RecitationControl.Finish] and the server
     * acknowledges, or until collection is cancelled.
     *
     * Cold: nothing opens the microphone or the socket until collection starts, and both are
     * released when it ends, however it ends.
     *
     * Prefer finishing over cancelling. `Finish` lets the server flush the in-progress utterance,
     * which is where the last few seconds of the recitation are; cancelling drops them.
     */
    fun session(
        config: LiveRecitationConfig,
        controls: Flow<RecitationControl>,
    ): Flow<LiveRecitationEvent>
}
