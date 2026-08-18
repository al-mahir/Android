package com.iti.meeting.presentation.call.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "MeetingCallAudio"

data class AudioRouteState(
    val available: List<AudioOutputDevice> = listOf(AudioOutputDevice.SPEAKER),
    val selected: AudioOutputDevice = AudioOutputDevice.SPEAKER,
    val userSelected: Boolean = false,
)


class AudioRouteController(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _state = MutableStateFlow(AudioRouteState())
    val state: StateFlow<AudioRouteState> = _state.asStateFlow()

    val currentDevice: AudioOutputDevice get() = _state.value.selected

    private var deviceCallback: AudioDeviceCallback? = null

    /** Starts watching for headset/Bluetooth connect+disconnect. Safe to call twice. */
    fun start() {
        refreshDevices()
        if (deviceCallback != null) return
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refreshDevices()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refreshDevices()
        }
        audioManager.registerAudioDeviceCallback(callback, null)
        deviceCallback = callback
    }

    /** Stops watching and resets to the default (speaker, no explicit user choice) for the next call. */
    fun stop() {
        deviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
        deviceCallback = null
        _state.value = AudioRouteState()
    }

    /** An explicit pick from the in-call output picker — sticky until that output disconnects. */
    fun select(device: AudioOutputDevice) {
        val current = _state.value
        if (device !in current.available) {
            Log.w(TAG, "select($device) ignored — not in available=${current.available}")
            return
        }
        Log.d(TAG, "select: $device (was ${current.selected})")
        _state.value = current.copy(selected = device, userSelected = true)
    }


    fun onEngineReportedRoute(device: AudioOutputDevice) {
        val current = _state.value
        if (current.selected == device) return
        Log.d(TAG, "onEngineReportedRoute: engine is on $device, state said ${current.selected}")
        // The engine can land on a route we hadn't listed yet (it sees hardware changes slightly
        // before AudioManager's callback fires); trust it and widen `available` rather than drop it.
        val available = if (device in current.available) current.available else current.available + device
        _state.value = current.copy(available = available, selected = device)
    }

    private fun refreshDevices() {
        val available = buildList {
            if (audioManager.hasEarpiece()) add(AudioOutputDevice.EARPIECE)
            add(AudioOutputDevice.SPEAKER)
            if (audioManager.hasType(WIRED_TYPES)) add(AudioOutputDevice.WIRED_HEADSET)
            if (audioManager.hasType(BLUETOOTH_TYPES)) add(AudioOutputDevice.BLUETOOTH)
        }
        val current = _state.value
        val selected = resolveSelection(current, available)
        Log.d(TAG, "refreshDevices: available=$available selected=$selected (was ${current.selected}, userSelected=${current.userSelected})")
        _state.value = AudioRouteState(
            available = available,
            selected = selected,
            // A user choice only survives while the device they chose is still plugged in.
            userSelected = current.userSelected && current.selected in available,
        )
    }


    private fun resolveSelection(
        current: AudioRouteState,
        available: List<AudioOutputDevice>,
    ): AudioOutputDevice {
        if (current.userSelected && current.selected in available) return current.selected
        return when {
            AudioOutputDevice.WIRED_HEADSET in available -> AudioOutputDevice.WIRED_HEADSET
            AudioOutputDevice.BLUETOOTH in available -> AudioOutputDevice.BLUETOOTH
            else -> AudioOutputDevice.SPEAKER
        }
    }

    private fun AudioManager.hasType(types: Set<Int>): Boolean =
        outputDevices().any { it.type in types }

    private fun AudioManager.hasEarpiece(): Boolean =
        outputDevices().any { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }

    private fun AudioManager.outputDevices(): Array<AudioDeviceInfo> =
        runCatching { getDevices(AudioManager.GET_DEVICES_OUTPUTS) }
            .onFailure { Log.w(TAG, "getDevices(GET_DEVICES_OUTPUTS) failed", it) }
            .getOrDefault(emptyArray())

    private companion object {
        val WIRED_TYPES: Set<Int> = buildSet {
            add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
            add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            add(AudioDeviceInfo.TYPE_USB_DEVICE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) add(AudioDeviceInfo.TYPE_USB_HEADSET)
        }

        val BLUETOOTH_TYPES: Set<Int> = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        )
    }
}
