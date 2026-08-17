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

/**
 * What outputs exist right now and which one the call should be using.
 *
 * [userSelected] is what makes the auto-routing policy in [AudioRouteController] non-annoying: once
 * the user has explicitly picked an output we stop overriding their choice on every device change,
 * and only fall back automatically when the device they picked physically disappears.
 */
data class AudioRouteState(
    val available: List<AudioOutputDevice> = listOf(AudioOutputDevice.SPEAKER),
    val selected: AudioOutputDevice = AudioOutputDevice.SPEAKER,
    val userSelected: Boolean = false,
)

/**
 * Tracks the connected audio outputs and decides which one a call should use.
 *
 * This owns *policy and availability only* — it never talks to Agora. [selected] is applied to the
 * engine by [com.iti.meeting.presentation.call.session.CallSessionController], which is the only
 * thing that knows whether an engine currently exists. That split keeps this class testable and
 * means a headset plugged in before the engine is up is still honoured once it comes up.
 *
 * Note on Agora's own routing: the SDK also switches routes on its own when hardware changes.
 * We don't fight it — [onEngineReportedRoute] folds whatever it actually did back into [state] so
 * the UI shows the truth rather than our intent.
 */
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

    /**
     * Folds the route Agora reports via `onAudioRouteChanged` back into [state] without marking it
     * as a user choice, so the UI reflects reality even when the SDK reroutes by itself.
     */
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

    /**
     * Routing policy, in priority order:
     * 1. Keep whatever the user explicitly picked, as long as it's still connected.
     * 2. Otherwise prefer a wired headset, then Bluetooth — plugging one in means "use this".
     * 3. Otherwise the speaker. Never the earpiece by default: this is a video call, and defaulting
     *    to the earpiece is exactly the "there's no sound" bug this whole class exists to fix.
     */
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
