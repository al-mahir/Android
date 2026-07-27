package com.iti.presentation.staticcontent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.core.getOrNull
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.usecase.legal.GetLegalDocumentUseCase
import com.iti.presentation.R
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.staticcontent.state.StaticContentEffect
import com.iti.presentation.staticcontent.state.StaticContentIntent
import com.iti.presentation.staticcontent.state.StaticContentUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class StaticContentViewModel(
    private val documentType: LegalDocumentType,
    private val getLegalDocument: GetLegalDocumentUseCase,
) : ViewModel(),
    StateHolder<StaticContentUiState> by DefaultStateHolder(StaticContentUiState()),
    EffectPublisher<StaticContentEffect> by DefaultEffectPublisher() {

    private var documentJob: Job? = null

    init {
        observeDocument()
    }

    fun onIntent(intent: StaticContentIntent) {
        when (intent) {
            StaticContentIntent.Retry -> observeDocument()
            StaticContentIntent.BackClicked -> sendEffect(StaticContentEffect.NavigateBack)
        }
    }

    private fun observeDocument() {
        // Cancel any in-flight collection so a retry cannot leave two streams writing state.
        documentJob?.cancel()
        updateState { copy(isLoading = true, errorMessageRes = null) }

        documentJob = getLegalDocument(documentType)
            .catch {
                updateState {
                    copy(isLoading = false, errorMessageRes = R.string.static_content_error_generic)
                }
            }
            .onEach { result ->
                val document = result.getOrNull()
                if (document == null) {
                    updateState {
                        copy(isLoading = false, errorMessageRes = R.string.static_content_error_generic)
                    }
                } else {
                    updateState {
                        copy(isLoading = false, errorMessageRes = null, document = document)
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
