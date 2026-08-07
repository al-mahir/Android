package com.iti.meeting.presentation.call

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.meeting.presentation.agora.AgoraEngineWrapper
import com.iti.meeting.presentation.call.session.CallSessionController
import com.iti.meeting.presentation.call.state.CallUiState
import com.iti.meeting.presentation.core.mvi.DefaultStateHolder
import com.iti.meeting.presentation.core.mvi.StateHolder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach


class CallViewModel(
    private val controller: CallSessionController,
) : ViewModel(), StateHolder<CallUiState> by DefaultStateHolder(controller.currentState.callState) {

    val engine: AgoraEngineWrapper? get() = controller.engine

    init {
        controller.state
            .map { it.callState }
            .onEach { updateState { it } }
            .launchIn(viewModelScope)
    }

    fun prepareForRequest(requestId: String) = controller.prepareForRequest(requestId)

    fun joinChannel(
        context: Context,
        requestId: String,
        token: String,
        channelName: String,
        userAccount: String,
        remoteDisplayName: String? = null,
        micEnabled: Boolean,
        cameraEnabled: Boolean,
    ) = controller.joinChannel(
        context = context,
        requestId = requestId,
        token = token,
        channelName = channelName,
        userAccount = userAccount,
        remoteDisplayName = remoteDisplayName,
        micEnabled = micEnabled,
        cameraEnabled = cameraEnabled,
    )

    fun endCall() = controller.endCall()

    fun toggleMic() = controller.toggleMic()

    fun toggleCamera() = controller.toggleCamera()

    fun toggleSpeaker() = controller.toggleSpeaker()

    fun switchCamera() = controller.switchCamera()
}
