package com.example.mushaf.domain.model.recite


data class RecitationSchema(
    val engines: List<RecitationEngineSpec>,
    val rules: List<TajweedRuleSpec>,
    val moshafFields: List<MoshafFieldSpec>,
)


data class RecitationEngineSpec(
    val key: String,
    val isDefault: Boolean,
) {

    val correctsRecitation: Boolean
        get() = corrects(key)

    companion object {
        const val FOLLOW_ALONG_ONLY_ENGINE = "zipformer"

        fun corrects(engineKey: String?): Boolean = engineKey != FOLLOW_ALONG_ONLY_ENGINE
    }
}

enum class TajweedRuleKind { TAJWEED, SIFA }

data class TajweedRuleSpec(
    val key: String,
    val label: String,
    val kind: TajweedRuleKind,
) {
    val requiresCorrectingEngine: Boolean
        get() = kind == TajweedRuleKind.SIFA
}


data class MoshafFieldSpec(
    val key: String,
    val label: String,
    val description: String?,
    val default: MoshafValue?,
    val options: List<MoshafOptionSpec>,
)

data class MoshafOptionSpec(
    val value: MoshafValue,
    val label: String,
)
