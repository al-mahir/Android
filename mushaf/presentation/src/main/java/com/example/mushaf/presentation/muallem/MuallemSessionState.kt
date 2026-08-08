package com.example.mushaf.presentation.muallem

import com.example.mushaf.domain.model.recite.RecitationWordFeedback

// ── Phase ─────────────────────────────────────────────────────────────────────

sealed interface MuallemPhase {
    /** Sheikh's audio is playing for the current ayah. */
    data object SheikhPlaying : MuallemPhase

    /** Mic is open — user is repeating (1-indexed). */
    data class UserRecording(val repeatIndex: Int) : MuallemPhase

    /** Brief feedback window between repeats or before auto-advancing. */
    data class ShowingFeedback(val repeatIndex: Int) : MuallemPhase
}

// ── Per-repeat record ──────────────────────────────────────────────────────────

data class MuallemRepeatFeedback(
    val repeatIndex: Int,
    val wordFeedback: Map<String, RecitationWordFeedback>,
) {
    val mistakeCount: Int get() = wordFeedback.values.count { it.countsAsMistake }
    val accuracy: Float
        get() {
            val total = wordFeedback.size
            if (total == 0) return 100f
            val correct = total - mistakeCount
            return (correct.toFloat() / total) * 100f
        }
}

// ── Session ────────────────────────────────────────────────────────────────────

data class MuallemSessionState(
    val surah: Int,
    val currentAyah: Int,
    val endAyah: Int,
    val difficulty: com.example.mushaf.domain.model.recite.RecitationStrictness,
    val repeatCount: Int,
    val currentRepeat: Int = 0,          // 0 = sheikh playing, 1..N = user repeating
    val phase: MuallemPhase = MuallemPhase.SheikhPlaying,
    val repeatFeedbacks: List<MuallemRepeatFeedback> = emptyList(),
    val accumulatedFeedbacks: List<MuallemRepeatFeedback> = emptyList(),
) {
    /** Ayah being practised, 1-based display. */
    val ayahDisplay: String get() = currentAyah.toString()

    /** The last completed repeat's feedback, for the UI to show accuracy. */
    val lastFeedback: MuallemRepeatFeedback? get() = repeatFeedbacks.lastOrNull()

    val isSessionActive: Boolean
        get() = phase != MuallemPhase.SheikhPlaying || currentRepeat > 0
}
