package com.example.mushaf.presentation.settings.recite

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mushaf.domain.model.recite.MoshafConstraints
import com.example.mushaf.domain.model.recite.RecitationSchema
import com.example.mushaf.domain.model.recite.RecitationSettings
import com.example.mushaf.domain.usecase.GetRecitationSchemaUseCase
import com.example.mushaf.domain.usecase.ObserveRecitationSettingsUseCase
import com.example.mushaf.domain.usecase.UpdateRecitationSettingsUseCase
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReciteSettingsViewModel(
    private val observeSettings: ObserveRecitationSettingsUseCase,
    private val updateSettings: UpdateRecitationSettingsUseCase,
    private val getSchema: GetRecitationSchemaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ReciteSettingsUiState())
    val state: StateFlow<ReciteSettingsUiState> = _state.asStateFlow()

    private var schema: RecitationSchema? = null
    private var settings = RecitationSettings()

    init {
        observeSettings()
            .catch { Log.e(TAG, "Failed to read recitation settings", it) }
            .onEach { stored ->
                settings = stored
                _state.update { it.applying(stored) }
            }
            .launchIn(viewModelScope)

        loadSchema()
    }

    fun onIntent(intent: ReciteSettingsIntent) {
        when (intent) {
            // Engine and practice mode are one setting seen from two screens, so each entry
            // point moves both - see RecitationSettings.withEngine.
            is ReciteSettingsIntent.SelectEngine -> persist { withEngine(intent.key) }

            is ReciteSettingsIntent.SelectStrictness ->
                persist { copy(strictness = intent.strictness) }

            is ReciteSettingsIntent.SetTajweedGrading ->
                persist { withTajweedGrading(intent.enabled) }

            is ReciteSettingsIntent.ToggleRule -> persist {
                val current = gradedRules ?: schema?.rules?.map { it.key }?.toSet() ?: emptySet()
                copy(
                    gradedRules = if (intent.key in current) {
                        current - intent.key
                    } else {
                        current + intent.key
                    },
                )
            }

            ReciteSettingsIntent.GradeAllRules -> persist { copy(gradedRules = null) }

            is ReciteSettingsIntent.SetMoshafValue -> persist {
                copy(moshaf = MoshafConstraints.apply(moshaf, intent.key, intent.value))
            }

            ReciteSettingsIntent.ResetMoshaf -> persist { copy(moshaf = emptyMap()) }

            ReciteSettingsIntent.ToggleAdvancedMoshaf -> _state.update {
                it.copy(isAdvancedMoshafExpanded = !it.isAdvancedMoshafExpanded)
            }

            ReciteSettingsIntent.Retry -> loadSchema()
        }
    }

    private fun loadSchema() {
        _state.update { it.copy(isLoading = true, isUnavailable = false) }
        viewModelScope.launch {
            when (val result = getSchema()) {
                is Result.Success -> {
                    schema = result.data
                    _state.update { it.applying(settings, result.data) }
                }

                is Result.Error -> {
                    Log.w(TAG, "Recitation schema unavailable: ${result.error}")
                    _state.update { it.copy(isLoading = false, isUnavailable = true) }
                }
            }
        }
    }


    private fun persist(mutate: RecitationSettings.() -> RecitationSettings) {
        val updated = settings.mutate()
        if (updated == settings) return
        viewModelScope.launch {
            runCatching { updateSettings(updated) }
                .onFailure { Log.e(TAG, "Failed to store recitation settings", it) }
        }
    }

    private fun ReciteSettingsUiState.applying(
        stored: RecitationSettings,
        loaded: RecitationSchema? = schema,
    ): ReciteSettingsUiState = copy(
        isLoading = loaded == null && !isUnavailable,
        isUnavailable = if (loaded != null) false else isUnavailable,
        engines = loaded?.engines.orEmpty().map { engine ->
            EngineOptionUi(
                key = engine.key,
                isSelected = engine.key == stored.wireEngine,
                correctsRecitation = engine.correctsRecitation,
            )
        },
        strictness = stored.strictness,
        tajweedGradingEnabled = stored.gradesTajweed,
        rules = loaded?.rules.orEmpty(),
        selectedRules = stored.gradedRules,
        moshafFields = loaded?.moshafFields.orEmpty(),
        moshafSelection = stored.moshaf,
    )

    private companion object {
        const val TAG = "Mushaf"
    }
}
