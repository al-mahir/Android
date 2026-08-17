package com.iti.meeting.presentation.call.audio

import io.agora.rtc2.Constants

/**
 * The audio output routes a user can pick between during a call.
 *
 * Deliberately coarse — one entry per *kind* of output rather than per physical device. Android
 * can report several `AudioDeviceInfo`s that all mean "the Bluetooth headset the user is wearing"
 * (SCO + A2DP for the same headset), and Agora routes by kind anyway
 * ([io.agora.rtc2.Constants.AUDIO_ROUTE_BLUETOOTH_DEVICE_HFP], not by device id), so modelling
 * individual devices would add a picker full of duplicates that all do the same thing.
 */
enum class AudioOutputDevice {
    EARPIECE,
    SPEAKER,
    WIRED_HEADSET,
    BLUETOOTH,
    ;

    /** The `Constants.AUDIO_ROUTE_*` value to hand [io.agora.rtc2.RtcEngine.setRouteInCommunicationMode]. */
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
