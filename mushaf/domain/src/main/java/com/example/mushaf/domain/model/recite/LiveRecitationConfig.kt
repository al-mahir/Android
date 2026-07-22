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
    val speechGate: SpeechGateConfig = SpeechGateConfig(),
)
