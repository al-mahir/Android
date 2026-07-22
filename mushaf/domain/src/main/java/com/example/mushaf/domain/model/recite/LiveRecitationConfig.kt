package com.example.mushaf.domain.model.recite

/**
 * How harshly to grade.
 *
 * It does **not** change what counts as a mistake — the diff finds identical errors at every
 * level. It sets the confidence threshold at which a finding is reported as a hard mistake
 * instead of being softened to a hint, so a *lower* threshold is a harsher teacher.
 *
 * The underlying thresholds are documented as uncalibrated placeholders awaiting a labelled set.
 * Treat the ordering as meaningful and do not build a user-facing accuracy claim on them.
 */
enum class RecitationStrictness(val wireValue: String) {
    LENIENT("lenient"),
    NORMAL("normal"),
    STRICT("strict"),
}

/**
 * A moshaf setting's value, which the schema defines as either a string or an integer per field.
 *
 * Modelled here rather than leaking a JSON type into the domain.
 */
sealed interface MoshafValue {
    data class Text(val value: String) : MoshafValue
    data class Number(val value: Int) : MoshafValue
}

/**
 * Everything a live session needs to start.
 *
 * @param start where the reciter is. Send it whenever known — with a cursor each chunk is matched
 *   by a cheap windowed search and is structurally immune to mutashābihāt. Null selects the
 *   deliberate "just start reciting, find me" mode, which must be prepared to show candidates.
 * @param gradedRules tajwīd rule keys to grade; null grades everything, and an **empty set** is a
 *   real choice meaning "no tajwīd rule at all, ḥifẓ and tashkīl only". Ḥifẓ and tashkīl are
 *   never filtered whatever this holds.
 * @param engine null uses the server default. An unbuilt engine falls back silently, so compare
 *   this against what the session ack reports.
 * @param moshaf only the fields the reciter changed; the server layers them over its defaults.
 *   An out-of-range value makes it discard the **whole** object, which looks exactly like the
 *   setting being ignored.
 */
data class LiveRecitationConfig(
    val start: RecitationCursor?,
    val strictness: RecitationStrictness = RecitationStrictness.NORMAL,
    val engine: String? = null,
    val gradedRules: Set<String>? = null,
    val moshaf: Map<String, MoshafValue> = emptyMap(),
    val speechGate: SpeechGateConfig = SpeechGateConfig(),
)
