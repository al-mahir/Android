package com.iti.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iti.domain.model.home.HomeSummary
import com.iti.domain.usecase.home.GetHomeSummaryUseCase
import com.iti.domain.usecase.home.JoinCircleUseCase
import com.iti.presentation.R
import com.iti.presentation.core.mvi.DefaultEffectPublisher
import com.iti.presentation.core.mvi.DefaultStateHolder
import com.iti.presentation.core.mvi.EffectPublisher
import com.iti.presentation.core.mvi.StateHolder
import com.iti.presentation.home.state.HomeEffect
import com.iti.presentation.home.state.HomeIntent
import com.iti.presentation.home.state.HomeUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Home screen ViewModel.
 *
 * State and effect plumbing come from the shared MVI delegates rather than hand-rolled
 * `MutableStateFlow`/`Channel` fields, so this class only holds Home's own reducer logic.
 * It depends on use cases only — never on a repository or data source.
 */
class HomeViewModel(
    private val getHomeSummary: GetHomeSummaryUseCase,
    private val joinCircle: JoinCircleUseCase,
) : ViewModel(),
    StateHolder<HomeUiState> by DefaultStateHolder(HomeUiState()),
    EffectPublisher<HomeEffect> by DefaultEffectPublisher() {

    private var summaryJob: Job? = null

    init {
        observeSummary()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Retry -> observeSummary()
            HomeIntent.SearchClicked -> sendEffect(HomeEffect.OpenSearch)
            HomeIntent.ProfileClicked -> sendEffect(HomeEffect.OpenProfile)
            HomeIntent.SeeAllSheikhsClicked -> sendEffect(HomeEffect.OpenSheikhList)
            HomeIntent.SeeAllCirclesClicked -> sendEffect(HomeEffect.OpenCircleList)
            HomeIntent.ContinueReadingClicked -> openContinueReading()
            is HomeIntent.SheikhClicked -> sendEffect(HomeEffect.OpenSheikh(intent.sheikhId))
            is HomeIntent.JoinCircleClicked -> join(intent.circleId)
        }
    }

    private fun observeSummary() {
        // Cancel any in-flight collection so a retry cannot leave two streams writing state.
        summaryJob?.cancel()
        updateState { copy(isLoading = true, errorMessageRes = null) }

        summaryJob = getHomeSummary()
            .catch { updateState { copy(isLoading = false, errorMessageRes = R.string.home_error_generic) } }
            .onEach { summary -> updateState { reduce(summary) } }
            .launchIn(viewModelScope)
    }

    private fun HomeUiState.reduce(summary: HomeSummary): HomeUiState = copy(
        isLoading = false,
        errorMessageRes = null,
        user = summary.user,
        continueReading = summary.continueReading,
        sheikhs = summary.sheikhs,
        circles = summary.circles,
    )

    private fun openContinueReading() {
        val page = currentState.continueReading?.pageNumber ?: return
        sendEffect(HomeEffect.OpenMushafAtPage(page))
    }

    private fun join(circleId: String) {
        // Ignore repeat taps while the request is in flight.
        if (circleId in currentState.joiningCircleIds) return
        updateState { copy(joiningCircleIds = joiningCircleIds + circleId) }

        viewModelScope.launch {
            runCatching { joinCircle(circleId) }
                .onFailure { sendEffect(HomeEffect.ShowMessage(R.string.home_join_failed)) }
            // The repository stream re-emits the joined circle, so state needs no local patch —
            // only the pending marker is cleared here.
            updateState { copy(joiningCircleIds = joiningCircleIds - circleId) }
        }
    }
}
