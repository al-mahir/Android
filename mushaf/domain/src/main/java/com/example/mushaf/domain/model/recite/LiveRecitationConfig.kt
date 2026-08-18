package com.example.mushaf.domain.model.recite










 
enum class RecitationStrictness(val wireValue: String) {
    LENIENT("lenient"),
    NORMAL("normal"),
    STRICT("strict"),
}





 
sealed interface MoshafValue {
    data class Text(val value: String) : MoshafValue
    data class Number(val value: Int) : MoshafValue
}















 
data class LiveRecitationConfig(
    val start: RecitationCursor?,
    val strictness: RecitationStrictness = RecitationStrictness.NORMAL,
    val engine: String? = null,
    val gradedRules: Set<String>? = null,
    val moshaf: Map<String, MoshafValue> = emptyMap(),
    /**
     * Off by default: the audio the server grades is sent continuously.
     *
     * The server endpoints with Silero, counting silence in *received samples*, so anything we
     * withhold edits the timeline it reasons over. A client-side RMS gate on top of that costs
     * more than the ~32kB/s it saves - it flushes its pre-roll as a burst after a gap (the worst
     * possible input shape for a stateful RNN endpointer), it splices silence out from under the
     * server's counter, and it drops a trailing madd decaying below the noise floor, moving where
     * the waqf appears to be. The gate still runs; it just no longer withholds. See
     * `docs/features/06-taahud-highlight-latency-mobile-plan.md` §3.3.
     */
    val speechGate: SpeechGateConfig = SpeechGateConfig.Disabled,
)
