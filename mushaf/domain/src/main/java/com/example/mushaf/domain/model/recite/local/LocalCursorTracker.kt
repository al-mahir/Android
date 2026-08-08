package com.example.mushaf.domain.model.recite.local

/**
 * Bookkeeping for the "a locally recognized word moves the highlight" path: holds a small
 * lookahead window of expected words anchored at a known-good position, plus a bias index
 * tracking where within that window the reciter is currently believed to be. [offer] is
 * structurally incapable of guessing - it only ever returns a real window entry's word id (via
 * [LocalRecitationCursor.resolve]) or `null` on no match, so callers can treat a non-match as a
 * silent no-op, never a fallback to a timing estimate.
 *
 * Kept deliberately dumb about *where its words come from* - callers own re-seeding the window
 * (on page load, session start, and on every server-confirmed chunk, so it keeps sliding forward
 * with ground truth rather than sitting fixed at a large, increasingly stale span).
 */
class LocalCursorTracker {

    private var window: List<LocalWordEntry> = emptyList()
    private var biasIndex: Int = 0

    /** The window entry [biasIndex] currently points at, or `null` if the window is empty/unset. */
    val currentWordId: String?
        get() = window.getOrNull(biasIndex)?.wordId

    /**
     * Replaces the tracking window. [biasIndex] re-anchors at [anchorWordId]'s position in
     * [newWindow] if it's present there, or falls back to the window's first entry otherwise (a
     * fresh window built around a cursor that isn't literally in its own word list, which
     * shouldn't normally happen but shouldn't crash if it does).
     */
    fun setWindow(newWindow: List<LocalWordEntry>, anchorWordId: String) {
        window = newWindow
        val anchorIndex = newWindow.indexOfFirst { it.wordId == anchorWordId }
        biasIndex = if (anchorIndex >= 0) anchorIndex else 0
    }

    /**
     * Offers one locally recognized word. Always resolves in [ArabicPhoneticMatcher]'s `strict`
     * mode - this fires on every single word with no consecutive-agreement check (unlike
     * [RecitationStartDetector]'s multi-word lock, which can afford the full loose fallback
     * tiers), so it can't tolerate their false-positive rate. Returns the matched word id and
     * advances [biasIndex] to it, or `null` on no match, leaving [biasIndex] untouched.
     */
    fun offer(word: String): String? {
        val matchIndex = LocalRecitationCursor.resolve(word, window, biasIndex, strict = true) ?: return null
        biasIndex = matchIndex
        return window[matchIndex].wordId
    }

    fun reset() {
        window = emptyList()
        biasIndex = 0
    }
}
