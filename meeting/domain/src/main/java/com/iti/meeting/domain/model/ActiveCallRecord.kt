package com.iti.meeting.domain.model

/**
 * Locally-persisted record of a call the current user had joined — the only thing that survives
 * real process death (see `docs/Meeting-Call-Lifecycle-Plan.md`; `CallSessionController`'s
 * in-memory state does not). Saved the moment a call is joined, cleared on any terminal state.
 * `channelName`/`userAccount` are carried only as a display/rejoin fallback — a rejoin always
 * fetches a fresh token via `MeetingRepository.refreshToken` rather than trusting these, since
 * they may be stale by the time the app is relaunched.
 */
data class ActiveCallRecord(
    val requestId: String,
    val channelName: String,
    val userAccount: String,
    val remoteDisplayName: String?,
)
