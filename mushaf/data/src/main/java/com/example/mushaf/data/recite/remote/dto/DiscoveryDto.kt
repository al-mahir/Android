package com.example.mushaf.data.recite.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * `GET /health`. Call at app start and cache.
 *
 * [availableEngines] is the only honest source for an engine picker — this deployment reports
 * `["real"]` alone, with no `mock` and no `zipformer`.
 */
@Serializable
data class HealthDto(
    val status: String,
    /** The server default, used when a session does not request one. */
    val engine: String,
    @SerialName("available_engines") val availableEngines: List<String> = emptyList(),
    val device: String? = null,
    val dtype: String? = null,
    @SerialName("muaalem_model") val muaalemModel: String? = null,
    @SerialName("segmenter_model") val segmenterModel: String? = null,
)

/** `GET /tajweed-rules`. Keys feed the `rules` leniency array of the start message. */
@Serializable
data class TajweedRulesResponseDto(
    val rules: List<TajweedRuleDto> = emptyList(),
)

@Serializable
data class TajweedRuleDto(
    /** Key off this, never off the display name. */
    val key: String,
    @SerialName("name_ar") val nameAr: String,
    @SerialName("name_en") val nameEn: String? = null,
    /** `tajweed` or `sifa`. Note `ghonna` is a ṣifā, and zipformer produces no ṣifā findings. */
    val kind: String? = null,
)

/**
 * `GET /moshaf-schema`. Build the settings panel from this response — the legal ranges are not
 * uniform (`madd_monfasel_len` is 2–5, `madd_mottasel_len` is 4–6), and an out-of-range value
 * makes the server discard the entire moshaf object.
 */
@Serializable
data class MoshafSchemaDto(
    val fields: List<MoshafFieldDto> = emptyList(),
)

@Serializable
data class MoshafFieldDto(
    val key: String,
    @SerialName("name_ar") val nameAr: String? = null,
    val description: String? = null,
    /**
     * A sensible starting value for the picker — and **not always what the server grades with**
     * when `moshaf` is omitted. `madd_monfasel_len` reports 2 here while the server's own
     * default is 4, so send the field explicitly if the displayed value must match the graded one.
     */
    val default: JsonPrimitive? = null,
    val options: List<MoshafOptionDto> = emptyList(),
)

/** [value] is a string or an integer depending on the field; [label] is display-ready Arabic. */
@Serializable
data class MoshafOptionDto(
    val value: JsonPrimitive,
    val label: String,
)
