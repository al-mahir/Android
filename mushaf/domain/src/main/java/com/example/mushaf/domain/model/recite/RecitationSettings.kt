package com.example.mushaf.domain.model.recite


data class RecitationSettings(
    val engine: String? = null,
    val strictness: RecitationStrictness = RecitationStrictness.NORMAL,

    val tajweedGradingEnabled: Boolean = true,

    val gradedRules: Set<String>? = null,
    val moshaf: Map<String, MoshafValue> = emptyMap(),
) {

    val wireRules: Set<String>?
        get() = if (tajweedGradingEnabled) gradedRules else emptySet()


    val engineCanGradeTajweed: Boolean
        get() = RecitationEngineSpec.corrects(engine)

    fun toConfig(from: RecitationCursor?): LiveRecitationConfig = LiveRecitationConfig(
        start = from,
        strictness = strictness,
        engine = engine,
        gradedRules = wireRules,
        moshaf = moshaf,
    )
}
