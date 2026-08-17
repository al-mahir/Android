package com.iti.meeting.presentation.agora

import android.content.Context
import android.util.Log
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.UserInfo
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.CameraCapturerConfiguration.CAMERA_DIRECTION

private const val TAG = "MeetingCall"

/**
 * @param audioOnly skips the video pipeline entirely — used by group circles, which are an audio
 *   roster with no camera. Starting the video engine for them would burn battery and, on some
 *   devices, take the camera exclusively for nothing.
 */
class AgoraEngineWrapper(
    context: Context,
    appId: String,
    listener: IRtcEngineEventHandler,
    private val audioOnly: Boolean = false,
) {

    val engine: RtcEngine = RtcEngine.create(context, appId, listener).apply {
        setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        if (audioOnly) {
            enableAudio()
        } else {
            enableVideo()
            enableLocalVideo(false)
        }
        enableLocalAudio(false)
        setDefaultAudioRoutetoSpeakerphone(true)
    }

    fun joinChannel(token: String, channelName: String, userAccount: String, publishAudio: Boolean, publishVideo: Boolean) {
        setLocalAudioEnabled(publishAudio)
        if (!audioOnly) setLocalVideoEnabled(publishVideo)

        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            publishMicrophoneTrack = publishAudio
            publishCameraTrack = publishVideo && !audioOnly
            autoSubscribeAudio = true
            autoSubscribeVideo = !audioOnly
        }
        val result = engine.joinChannelWithUserAccount(token, channelName, userAccount, options)
        Log.i(TAG, "engine.joinChannelWithUserAccount(channel=$channelName, userAccount=$userAccount, audioOnly=$audioOnly) returned $result (0 = accepted)")
    }

    /**
     * Turns on periodic loudness reports so the roster can highlight whoever is speaking.
     * `reportVad` adds voice-activity detection for the local user, which is what stops the local
     * "speaking" ring from lighting up on keyboard clicks and background noise.
     */
    fun enableSpeakingIndication(intervalMs: Int = SPEAKING_INTERVAL_MS) {
        engine.enableAudioVolumeIndication(intervalMs, SPEAKING_SMOOTHING, true)
    }

    /**
     * Resolves an Agora uid to the `userAccount` the channel was joined with, so a uid from an SDK
     * callback can be matched against the roster. Returns null when the SDK hasn't learned the
     * mapping yet — callers should fall back to `onUserInfoUpdated`.
     */
    fun userAccountFor(uid: Int): String? {
        val info = UserInfo()
        val result = engine.getUserInfoByUid(uid, info)
        return if (result == 0) info.userAccount?.takeIf { it.isNotBlank() } else null
    }

    fun renewToken(token: String) {
        engine.renewToken(token)
    }

    fun setLocalAudioEnabled(enabled: Boolean) {
        engine.enableLocalAudio(enabled)
        engine.updateChannelMediaOptions(ChannelMediaOptions().apply { publishMicrophoneTrack = enabled })
    }

    fun setLocalVideoEnabled(enabled: Boolean) {
        engine.enableLocalVideo(enabled)
        if (enabled) engine.startPreview() else engine.stopPreview()
        engine.updateChannelMediaOptions(ChannelMediaOptions().apply { publishCameraTrack = enabled })
    }


    fun setAudioRoute(agoraRoute: Int) {
        val result = engine.setRouteInCommunicationMode(agoraRoute)
        Log.i(TAG, "setRouteInCommunicationMode($agoraRoute) returned $result (0 = ok)")
        if (agoraRoute == Constants.AUDIO_ROUTE_SPEAKERPHONE || agoraRoute == Constants.AUDIO_ROUTE_EARPIECE) {
            engine.setEnableSpeakerphone(agoraRoute == Constants.AUDIO_ROUTE_SPEAKERPHONE)
        }
    }


    fun setCameraDirection(front: Boolean) {
        val direction = if (front) CAMERA_DIRECTION.CAMERA_FRONT else CAMERA_DIRECTION.CAMERA_REAR
        val result = engine.setCameraCapturerConfiguration(CameraCapturerConfiguration(direction))
        Log.i(TAG, "setCameraCapturerConfiguration(front=$front) returned $result (0 = ok)")
    }

    fun switchCamera() {
        engine.switchCamera()
    }

    /** The torch is a rear-camera feature on essentially every phone, and unsupported on many. */
    fun isTorchSupported(): Boolean = runCatching { engine.isCameraTorchSupported() }.getOrDefault(false)

    fun setTorchEnabled(enabled: Boolean) {
        val result = engine.setCameraTorchOn(enabled)
        Log.i(TAG, "setCameraTorchOn($enabled) returned $result (0 = ok)")
    }

    fun leaveChannel() = engine.leaveChannel()

    fun destroy() = RtcEngine.destroy()

    private companion object {
        const val SPEAKING_INTERVAL_MS = 400
        /** Agora's smoothing factor; 3 is the SDK's documented default. */
        const val SPEAKING_SMOOTHING = 3
    }
}
