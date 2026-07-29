package com.example.mushaf.presentation.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.model.ResourceKind
import com.example.mushaf.domain.usecase.CancelResourceDownloadUseCase
import com.example.mushaf.domain.usecase.DeleteResourceDownloadUseCase
import com.example.mushaf.domain.usecase.ObserveDownloadableResourcesUseCase
import com.example.mushaf.domain.usecase.StartResourceDownloadUseCase
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.core.mvi.DefaultEffectPublisher
import com.example.mushaf.presentation.core.mvi.DefaultStateHolder
import com.example.mushaf.presentation.core.mvi.EffectPublisher
import com.example.mushaf.presentation.core.mvi.StateHolder
import com.example.mushaf.presentation.download.state.DownloadsEffect
import com.example.mushaf.presentation.download.state.DownloadsIntent
import com.example.mushaf.presentation.download.state.DownloadsUiState
import com.iti.domain.core.fold
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val kind: ResourceKind,
    private val observeResources: ObserveDownloadableResourcesUseCase,
    private val startDownload: StartResourceDownloadUseCase,
    private val cancelDownload: CancelResourceDownloadUseCase,
    private val deleteDownload: DeleteResourceDownloadUseCase,
) : ViewModel(),
    StateHolder<DownloadsUiState> by DefaultStateHolder(DownloadsUiState()),
    EffectPublisher<DownloadsEffect> by DefaultEffectPublisher() {

    private var resourcesJob: Job? = null

    init {
        updateState { copy(kind = this@DownloadsViewModel.kind) }
        observeCatalogue()
    }

    fun onIntent(intent: DownloadsIntent) {
        when (intent) {
            is DownloadsIntent.DownloadClicked -> viewModelScope.launch { startDownload(intent.id) }
            is DownloadsIntent.CancelClicked -> viewModelScope.launch { cancelDownload(intent.id) }
            is DownloadsIntent.DeleteClicked -> confirmDeletion(intent.id)
            DownloadsIntent.DeleteConfirmed -> performDeletion()
            DownloadsIntent.DeleteDismissed -> updateState { copy(pendingDeletion = null) }
            DownloadsIntent.Retry -> observeCatalogue()
        }
    }


    private fun confirmDeletion(id: String) {
        val resource = currentState.resources.firstOrNull { it.id == id } ?: return
        updateState { copy(pendingDeletion = resource) }
    }

    private fun performDeletion() {
        val target = currentState.pendingDeletion ?: return
        updateState { copy(pendingDeletion = null) }
        viewModelScope.launch { deleteDownload(target.id) }
    }

    private fun observeCatalogue() {
        resourcesJob?.cancel()
        updateState { copy(isLoading = true, errorMessageRes = null) }

        resourcesJob = observeResources(kind)
            .onEach { result ->
                result.fold(
                    onSuccess = { resources ->
                        updateState { copy(isLoading = false, resources = resources) }
                    },
                    onError = {
                        updateState {
                            copy(isLoading = false, errorMessageRes = R.string.downloads_error_generic)
                        }
                        sendEffect(DownloadsEffect.ShowMessage(R.string.downloads_error_generic))
                    },
                )
            }
            .catch {
                updateState {
                    copy(isLoading = false, errorMessageRes = R.string.downloads_error_generic)
                }
                sendEffect(DownloadsEffect.ShowMessage(R.string.downloads_error_generic))
            }
            .launchIn(viewModelScope)
    }
}
