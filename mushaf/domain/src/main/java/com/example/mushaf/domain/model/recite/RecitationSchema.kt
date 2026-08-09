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

        /** The engine that grades tajwīd as well as memorisation. */
        const val CORRECTING_ENGINE = "real"

        fun corrects(engineKey: String?): Boolean = engineKey != FOLLOW_ALONG_ONLY_ENGINE

        /**
         * The engine a given practice mode needs. "Memorisation only" is not merely tajwīd grading
         * switched off — it is a different engine, and leaving the choice unsent let the server
         * pick its own default for both modes.
         */
        fun forTajweedGrading(enabled: Boolean): String =
            if (enabled) CORRECTING_ENGINE else FOLLOW_ALONG_ONLY_ENGINE
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
