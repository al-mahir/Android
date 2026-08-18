package com.iti.meeting.presentation.call.audio

import io.agora.rtc2.Constants


enum class AudioOutputDevice {
    EARPIECE,
    SPEAKER,
    WIRED_HEADSET,
    BLUETOOTH,
    ;

    val agoraRoute: Int
        get() = when (this) {
            EARPIECE -> Constants.AUDIO_ROUTE_EARPIECE
            SPEAKER -> Constants.AUDIO_ROUTE_SPEAKERPHONE
            WIRED_HEADSET -> Constants.AUDIO_ROUTE_HEADSET
            BLUETOOTH -> Constants.AUDIO_ROUTE_BLUETOOTH_DEVICE_HFP
        }

    companion object {
        /** Inverse of [agoraRoute], for reflecting what `onAudioRouteChanged` reports back into state. */
        fun fromAgoraRoute(route: Int): AudioOutputDevice? = when (route) {
            Constants.AUDIO_ROUTE_EARPIECE -> EARPIECE
            Constants.AUDIO_ROUTE_SPEAKERPHONE, Constants.AUDIO_ROUTE_LOUDSPEAKER -> SPEAKER
            Constants.AUDIO_ROUTE_HEADSET,
            Constants.AUDIO_ROUTE_HEADSETNOMIC,
            Constants.AUDIO_ROUTE_USB_HEADSET,
            Constants.AUDIO_ROUTE_USBDEVICE,
            -> WIRED_HEADSET

            Constants.AUDIO_ROUTE_BLUETOOTH_DEVICE_HFP,
            Constants.AUDIO_ROUTE_BLUETOOTH_DEVICE_A2DP,
            -> BLUETOOTH

            else -> null
        }
    }
}
