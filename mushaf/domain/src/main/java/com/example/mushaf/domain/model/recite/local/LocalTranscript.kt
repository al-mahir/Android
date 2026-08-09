package com.example.mushaf.domain.model.recite.local

/**
 * What the on-device model has heard so far in the current utterance, as an unbroken phoneme
 * string - the model's own alphabet has no word delimiter, so this is genuinely all it can say.
 *
 * [phonemes] is **cumulative and re-emitted on every decode**, not a delta: a streaming CTC
 * decoder can still revise its most recent output as more audio arrives, and re-sending the whole
 * utterance lets [PhonemeCursorTracker] realign rather than inherit a revision it never saw.
 * [isFinal] marks the last emission before the decoder resets on a detected pause, after which
 * [phonemes] starts over from empty.
 */
data class LocalTranscript(
    val phonemes: String,
    val isFinal: Boolean,
)
