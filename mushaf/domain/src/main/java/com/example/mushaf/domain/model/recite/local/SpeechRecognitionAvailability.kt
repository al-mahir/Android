package com.example.mushaf.domain.model.recite.local

/**
 * What kind of local speech recognition this device can actually do — deliberately distinct
 * from a boolean, since [ON_DEVICE] is the only value that keeps the live-follow cursor working
 * offline (the whole point of this spike per the plan in
 * docs/features/06-taahud-local-recitation-tracking-plan.md §2 Gap D).
 */
enum class SpeechRecognitionAvailability {
    /** Fully offline — Android's on-device recognizer is present for this locale. */
    ON_DEVICE,

    /** Recognition works but is cloud-backed, so it inherits the same connectivity dependency
     * this feature exists to remove. Useful for the spike's accuracy measurement, not for the
     * final offline-first design. */
    NETWORK_ONLY,

    /** No speech recognition available on this device at all. */
    UNAVAILABLE,
}
