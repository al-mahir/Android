package com.example.mushaf.presentation.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.model.LocalizedText
import com.example.mushaf.domain.usecase.CancelDownloadRecitationUseCase
import com.example.mushaf.domain.usecase.DownloadRecitationUseCase
import com.example.mushaf.domain.usecase.GetDownloadProgressUseCase
import com.example.mushaf.domain.usecase.GetRecitersUseCase
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.core.mvi.DefaultEffectPublisher
import com.example.mushaf.presentation.core.mvi.DefaultStateHolder
import com.example.mushaf.presentation.core.mvi.EffectPublisher
import com.example.mushaf.presentation.core.mvi.StateHolder
import com.example.mushaf.presentation.download.state.SurahDownloadEffect
import com.example.mushaf.presentation.download.state.SurahDownloadIntent
import com.example.mushaf.presentation.download.state.SurahDownloadItem
import com.example.mushaf.presentation.download.state.SurahDownloadUiState
import com.example.mushaf.presentation.SurahNameResolver
import com.iti.domain.core.fold
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SurahDownloadViewModel(
    private val reciterId: Int,
    private val downloadRecitation: DownloadRecitationUseCase,
    private val cancelDownloadRecitation: CancelDownloadRecitationUseCase,
    private val getDownloadProgress: GetDownloadProgressUseCase,
    private val getReciters: GetRecitersUseCase,
) : ViewModel(),
    StateHolder<SurahDownloadUiState> by DefaultStateHolder(SurahDownloadUiState(reciterId = reciterId)),
    EffectPublisher<SurahDownloadEffect> by DefaultEffectPublisher() {

    init {
        val initialSurahs = (1..114).map { surahNum ->
            SurahDownloadItem(
                surahNumber = surahNum,
                name = SurahNameResolver.nameFor(surahNum),
                status = null
            )
        }
        updateState { copy(surahs = initialSurahs) }

        getReciters()
            .onEach { result ->
                result.fold(
                    onSuccess = { reciters ->
                        val reciter = reciters.find { it.id == reciterId }
                        if (reciter != null) {
                            updateState {
                                copy(reciterName = LocalizedText(arabic = reciter.nameArabic, english = reciter.name))
                            }
                        }
                    },
                    onError = {}
                )
            }
            .launchIn(viewModelScope)

        getDownloadProgress(reciterId)
            .onEach { allStatuses ->
                val statuses = allStatuses.filter { it.reciterId == reciterId }
                updateState {
                    val isFullCompleted = statuses.any { it.id == "${reciterId}_full" && it.isCompleted }
                    val newSurahs = surahs.map { item ->
                        val status = statuses.find { it.surahId == item.surahNumber }
                        val isCompleted = status?.isCompleted == true || isFullCompleted
                        item.copy(
                            status = if (isCompleted) {
                                status?.copy(state = com.example.mushaf.domain.model.DownloadStatus.STATE_COMPLETED, progress = 100)
                                    ?: com.example.mushaf.domain.model.DownloadStatus(
                                        id = "${reciterId}_${item.surahNumber}",
                                        reciterId = reciterId,
                                        surahId = item.surahNumber,
                                        progress = 100,
                                        state = com.example.mushaf.domain.model.DownloadStatus.STATE_COMPLETED,
                                        errorMessage = null
                                    )
                            } else {
                                status
                            },
                            isDownloading = !isCompleted && (status?.isDownloading == true),
                            progress = if (isCompleted) 100 else (status?.progress ?: 0)
                        )
                    }
                    copy(surahs = newSurahs)
                }
            }
            .catch { e ->
                sendEffect(SurahDownloadEffect.ShowError(R.string.downloads_error_generic))
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: SurahDownloadIntent) {
        when (intent) {
            is SurahDownloadIntent.DownloadSurah -> {
                viewModelScope.launch {
                    downloadRecitation(reciterId, intent.surahNumber)
                }
            }
            is SurahDownloadIntent.CancelDownload -> {
                viewModelScope.launch {
                    cancelDownloadRecitation(reciterId, intent.surahNumber)
                }
            }
        }
    }
}
