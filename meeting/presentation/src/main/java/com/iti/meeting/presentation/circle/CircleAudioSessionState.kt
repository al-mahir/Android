package com.iti.meeting.presentation.circle

import com.iti.meeting.presentation.call.audio.AudioOutputDevice

/**
 * Live Agora state for a circle's group audio session.
 *
 * Keyed by Agora `uid`, not by roster `userId`: uids are what every SDK callback reports, and the
 * mapping to a roster identity only arrives asynchronously (see [CircleParticipantAudio.userAccount]).
 * [CircleAudioSessionController.participantsByUserAccount] does the join for the UI layer.
 */
data class CircleAudioSessionState(
    val circleId: String? = null,
    val status: CircleAudioStatus = CircleAudioStatus.Idle,
    val isMicEnabled: Boolean = false,
    val audioDevice: AudioOutputDevice = AudioOutputDevice.SPEAKER,
    val availableAudioDevices: List<AudioOutputDevice> = listOf(AudioOutputDevice.SPEAKER),
    val remoteParticipants: Map<Int, CircleParticipantAudio> = emptyMap(),
    /** Agora uid of whoever is currently loudest, or null when nobody is speaking. */
    val activeSpeakerUid: Int? = null,
    val isLocalSpeaking: Boolean = false,
    val sessionDurationSeconds: Long = 0L,
) {
    val isLive: Boolean get() = status is CircleAudioStatus.Live || status is CircleAudioStatus.Connecting

    /**
     * Live audio state addressed by roster identity, for merging into the participant list.
     * Entries whose uid hasn't been resolved to a `userAccount` yet are simply absent — the roster
     * still renders, just without live indicators for that person until the mapping arrives.
     */
    val participantsByUserAccount: Map<String, CircleParticipantAudio>
        get() = remoteParticipants.values
            .mapNotNull { participant -> participant.userAccount?.let { it to participant } }
            .toMap()
}

data class CircleParticipantAudio(
    val uid: Int,
    /** Resolved from `getUserInfoByUid`/`onUserInfoUpdated`; null until the SDK reports it. */
    val userAccount: String? = null,
    val isMicEnabled: Boolean = true,
    val isSpeaking: Boolean = false,
)

sealed interface CircleAudioStatus {
    data object Idle : CircleAudioStatus
    data object Connecting : CircleAudioStatus
    data object Live : CircleAudioStatus

    /** The circle isn't ONGOING, so the backend won't issue a token yet. Distinct from [Error]
     * because it is an expected, non-broken state the UI should explain rather than apologise for. */
    data object NotStarted : CircleAudioStatus
    data class Error(val reason: CircleAudioError) : CircleAudioStatus
}

enum class CircleAudioError {
    /** Token fetch failed, or the join never completed within the timeout. */
    CONNECT_FAILED,

    /** Was connected, then Agora reported the connection permanently failed. */
    CONNECTION_LOST,

    /** The user declined the microphone permission, so there is nothing to publish. */
    MIC_PERMISSION_DENIED,
}
