package com.iti.meeting.presentation.agora

import android.content.Context
import android.util.Log
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.ChannelMediaOptions

private const val TAG = "MeetingCall"

class AgoraEngineWrapper(context: Context, appId: String, listener: IRtcEngineEventHandler) {

    val engine: RtcEngine = RtcEngine.create(context, appId, listener).apply {
        setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        enableVideo()
        enableLocalAudio(false)
        enableLocalVideo(false)
        // CHANNEL_PROFILE_COMMUNICATION defaults to the earpiece on many devices, which reads as
        // "no audio at all" in a video call. Force the speaker so the remote party is audible.
        setDefaultAudioRoutetoSpeakerphone(true)
        setEnableSpeakerphone(true)
    }

    fun joinChannel(token: String, channelName: String, uid: Int, publishAudio: Boolean, publishVideo: Boolean) {
        setLocalAudioEnabled(publishAudio)
        setLocalVideoEnabled(publishVideo)

        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            publishMicrophoneTrack = publishAudio
            publishCameraTrack = publishVideo
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }
        val result = engine.joinChannel(token, channelName, uid, options)
        Log.i(TAG, "engine.joinChannel(channel=$channelName, uid=$uid) returned $result (0 = accepted)")
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

    fun switchCamera() {
        engine.switchCamera()
    }

    fun setSpeakerphoneEnabled(enabled: Boolean) {
        engine.setEnableSpeakerphone(enabled)
    }

    fun leaveChannel() = engine.leaveChannel()

    fun destroy() = RtcEngine.destroy()
}
