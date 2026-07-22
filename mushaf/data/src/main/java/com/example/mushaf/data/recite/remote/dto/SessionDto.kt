package com.example.mushaf.data.recite.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The `start` message — the first frame of a session, and the one that must be valid JSON text
 * or the server drops the connection (API.md §5.1).
 *
 * Every field except `type` is optional. Nulls are omitted at encode time rather than sent as
 * `null`, so an unset field means "server default" exactly as the contract describes.
 */
@Serializable
data class StartSessionDto(
    val type: String = "start",
    /** 1-based. Send it whenever known: it seeds the cursor and makes matching immune to mutashābihāt. */
    val sura: Int? = null,
    /** 1-based. */
    val aya: Int? = null,
    /** **0-based** within the āyah — unlike [sura] and [aya]. */
    @SerialName("word_idx") val wordIdx: Int? = null,
    /** `lenient` | `normal` | `strict`, lowercase. A mis-cased value silently falls back. */
    val strictness: String? = null,
    val engine: String? = null,
    /**
     * Tajwīd rules to grade. `null` grades everything; an empty list grades no tajwīd rule at
     * all (ḥifẓ and tashkīl only) and is a real choice, not a missing one.
     */
    val rules: List<String>? = null,
    /**
     * Reciter's tajwīd style, layered over the server's defaults. Values are string or int
     * depending on the field, hence [JsonElement]. An out-of-range value makes the server
     * discard the **whole** object, which looks exactly like the setting being ignored.
     */
    val moshaf: Map<String, JsonElement>? = null,
    /** Debug/research payload. Makes events considerably larger; not for rendering. */
    @SerialName("include_units") val includeUnits: Boolean? = null,
)

/**
 * The server's reply to [StartSessionDto].
 *
 * [engine] is the source of truth for what actually ran. Requesting an engine the server did not
 * build is **not** an error — it silently substitutes its default, and this field is the only
 * place that is visible.
 */
@Serializable
data class SessionAckDto(
    val type: String,
    @SerialName("session_id") val sessionId: String,
    val engine: String,
    @SerialName("sample_rate") val sampleRate: Int,
)

/** Repositions the cursor. No reply is sent; a message missing sura/aya is ignored silently. */
@Serializable
data class SeekDto(
    val type: String = "seek",
    val sura: Int,
    val aya: Int,
    @SerialName("word_idx") val wordIdx: Int = 0,
)

/** Ends the session. The server flushes, may push one last feedback event, then sends `done`. */
@Serializable
data class EndSessionDto(val type: String = "end")

/** Just enough of any inbound message to route it before committing to a full parse. */
@Serializable
data class MessageEnvelopeDto(val type: String? = null)
