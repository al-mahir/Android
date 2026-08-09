package com.example.mushaf.domain.model.recite


data class RecitationSettings(
    val engine: String? = null,
    val strictness: RecitationStrictness = RecitationStrictness.NORMAL,

    val tajweedGradingEnabled: Boolean = true,

    val gradedRules: Set<String>? = null,
    val moshaf: Map<String, MoshafValue> = emptyMap(),
) {

    /**
     * Whether tajwīd is actually being graded — the one value every screen should read.
     *
     * Derived rather than trusting [tajweedGradingEnabled] on its own: the flag and [engine] are
     * written together now, but settings persisted before that could hold "grade tajwīd" next to
     * an engine that cannot, and an engine that cannot grade tajwīd is the last word regardless of
     * what the flag says.
     */
    val gradesTajweed: Boolean
        get() = tajweedGradingEnabled && engineCanGradeTajweed

    val wireRules: Set<String>?
        get() = if (gradesTajweed) gradedRules else emptySet()


    /**
     * The engine actually sent to the server. Never null: with nothing stored the practice mode
     * decides, so a session always names its engine instead of taking whatever default the server
     * happens to run.
     */
    val wireEngine: String
        get() = engine ?: RecitationEngineSpec.forTajweedGrading(tajweedGradingEnabled)

    val engineCanGradeTajweed: Boolean
        get() = RecitationEngineSpec.corrects(wireEngine)

    /**
     * Switches practice mode, engine included.
     *
     * The mode toggle on the Mushaf screen and the engine picker in Recite Settings are two ways
     * into this one setting, so both go through here and neither can leave the pair disagreeing —
     * a reciter who picked "memorisation only" must not still be graded by the tajwīd engine.
     */
    fun withTajweedGrading(enabled: Boolean): RecitationSettings = copy(
        engine = RecitationEngineSpec.forTajweedGrading(enabled),
        tajweedGradingEnabled = enabled,
    )

    /** Switches engine, keeping the practice mode it implies in step. */
    fun withEngine(key: String): RecitationSettings = copy(
        engine = key,
        tajweedGradingEnabled = RecitationEngineSpec.corrects(key),
    )

    fun toConfig(from: RecitationCursor?): LiveRecitationConfig = LiveRecitationConfig(
        start = from,
        strictness = strictness,
        engine = wireEngine,
        gradedRules = wireRules,
        moshaf = moshaf,
    )
}
