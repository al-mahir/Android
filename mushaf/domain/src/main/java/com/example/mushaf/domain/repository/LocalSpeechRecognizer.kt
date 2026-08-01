package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.local.SpeechRecognitionAvailability
import kotlinx.coroutines.flow.Flow

/**
 * On-device speech-to-text, kept as narrow a contract as the live pipeline needs: settled words,
 * not raw partial-result churn — the debounce (last word unchanged for ~450ms, or the engine
 * marks it final) happens behind this interface, mirroring the iOS build's `SpeechRecognizer`.
 *
 * This is the Phase 2 spike from
 * docs/features/06-taahud-local-recitation-tracking-plan.md — answering whether Android's
 * built-in recognizer is accurate enough on Quranic recitation to be worth wiring into the live
 * cursor for real, before committing to a heavier engine (sherpa-onnx + a Qur'an-tuned model).
 */
interface LocalSpeechRecognizer {

    fun availability(): SpeechRecognitionAvailability

    /**
     * Starts listening and emits one settled word at a time until the collector cancels.
     * Throws if [availability] is [SpeechRecognitionAvailability.UNAVAILABLE] — callers must
     * check first, the same way [com.example.mushaf.domain.repository.LiveRecitationRepository]
     * callers are expected to hold RECORD_AUDIO before calling.
     */
    fun listen(): Flow<String>
}
