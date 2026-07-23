package com.example.mushaf.presentation.settings.recite

import com.example.mushaf.domain.model.recite.MoshafFieldSpec
import com.example.mushaf.domain.model.recite.MoshafValue
import com.example.mushaf.domain.model.recite.RecitationStrictness
import com.example.mushaf.domain.model.recite.TajweedRuleKind
import com.example.mushaf.domain.model.recite.TajweedRuleSpec

data class EngineOptionUi(
    val key: String,
    val isSelected: Boolean,
    val correctsRecitation: Boolean,
)

data class ReciteSettingsUiState(
    val isLoading: Boolean = true,
    val isUnavailable: Boolean = false,

    val engines: List<EngineOptionUi> = emptyList(),
    val strictness: RecitationStrictness = RecitationStrictness.NORMAL,

    val tajweedGradingEnabled: Boolean = true,
    val rules: List<TajweedRuleSpec> = emptyList(),
    val selectedRules: Set<String>? = null,

    val moshafFields: List<MoshafFieldSpec> = emptyList(),
    val moshafSelection: Map<String, MoshafValue> = emptyMap(),
    val isAdvancedMoshafExpanded: Boolean = false,
) {

    val engineCorrects: Boolean
        get() = engines.firstOrNull { it.isSelected }?.correctsRecitation ?: true

    val tajweedRules: List<TajweedRuleSpec>
        get() = rules.filter { it.kind == TajweedRuleKind.TAJWEED }

    val sifaRules: List<TajweedRuleSpec>
        get() = rules.filter { it.kind == TajweedRuleKind.SIFA }

    val commonMoshafFields: List<MoshafFieldSpec>
        get() = moshafFields.filter { it.key in COMMON_MOSHAF_KEYS }

    val advancedMoshafFields: List<MoshafFieldSpec>
        get() = moshafFields.filterNot { it.key in COMMON_MOSHAF_KEYS }

    private companion object {
        val COMMON_MOSHAF_KEYS = setOf(
            "recitation_speed",
            "madd_monfasel_len",
            "madd_mottasel_len",
            "madd_mottasel_waqf",
            "madd_aared_len",
            "madd_alleen_len",
        )
    }
}

sealed interface ReciteSettingsIntent {
    data class SelectEngine(val key: String) : ReciteSettingsIntent
    data class SelectStrictness(val strictness: RecitationStrictness) : ReciteSettingsIntent
    data class SetTajweedGrading(val enabled: Boolean) : ReciteSettingsIntent

    data class ToggleRule(val key: String) : ReciteSettingsIntent

    data object GradeAllRules : ReciteSettingsIntent

    data class SetMoshafValue(val key: String, val value: MoshafValue) : ReciteSettingsIntent

    data object ResetMoshaf : ReciteSettingsIntent

    data object ToggleAdvancedMoshaf : ReciteSettingsIntent
    data object Retry : ReciteSettingsIntent
}
