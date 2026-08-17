package com.iti.meeting.presentation.agora

import android.content.Context
import android.util.Log
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.CameraCapturerConfiguration.CAMERA_DIRECTION

private const val TAG = "MeetingCall"

class AgoraEngineWrapper(context: Context, appId: String, listener: IRtcEngineEventHandler) {

    val engine: RtcEngine = RtcEngine.create(context, appId, listener).apply {
        setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        enableVideo()
        enableLocalAudio(false)
        enableLocalVideo(false)
        setDefaultAudioRoutetoSpeakerphone(true)
    }

    fun joinChannel(token: String, channelName: String, userAccount: String, publishAudio: Boolean, publishVideo: Boolean) {
        setLocalAudioEnabled(publishAudio)
        setLocalVideoEnabled(publishVideo)

        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            publishMicrophoneTrack = publishAudio
            publishCameraTrack = publishVideo
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }
        val result = engine.joinChannelWithUserAccount(token, channelName, userAccount, options)
        Log.i(TAG, "engine.joinChannelWithUserAccount(channel=$channelName, userAccount=$userAccount) returned $result (0 = accepted)")
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
}
