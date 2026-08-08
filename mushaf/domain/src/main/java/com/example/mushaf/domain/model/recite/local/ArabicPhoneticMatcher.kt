package com.example.mushaf.domain.model.recite.local

/**
 * Decides whether a word an on-device ASR engine heard could plausibly be a given expected
 * Qur'an word — a tolerant text-comparison, not a correctness grader. Ported from the shape of
 * the iOS build's `ArabicPhoneticMatcher` (normalize -> exact -> affix-strip -> phonetic-group
 * substitution -> edit distance -> soundex), generalized to the whole Qur'an: the iOS version's
 * `commonMisrecognitions` dictionary was hand-authored for Al-Fātiḥa only (~30 entries) and does
 * not extend to the other ~77,000 words, so it is intentionally not ported. If specific recurring
 * ASR confusions turn out to matter, mine them from session data rather than hand-guessing them.
 *
 * This never asserts correctness — callers use it only to move a live reading-position estimate,
 * matching [com.example.mushaf.domain.model.recite.RecitationPacer]'s existing "position, never
 * correctness" invariant. Grading stays server-owned.
 */
object ArabicPhoneticMatcher {

    private val phoneticGroups: List<Set<Char>> = listOf(
        setOf('ح', 'ه', 'خ'),
        setOf('س', 'ص', 'ث'),
        setOf('ط', 'ت', 'د'),
        setOf('ظ', 'ز', 'ذ', 'ض'),
        setOf('ع', 'أ', 'ا', 'ء', 'غ'),
        setOf('ق', 'ك'),
        setOf('ف', 'ب', 'م'),
        setOf('ر', 'ز'),
        setOf('ي', 'ئ', 'ى'),
        setOf('و', 'ؤ'),
        setOf('ل', 'ن'),
    )

    private val phoneticGroupOf: Map<Char, Set<Char>> = buildMap {
        for (group in phoneticGroups) for (ch in group) put(ch, group)
    }

    private val prefixes = listOf("ال", "و", "ف", "ب", "ل", "ك", "س", "سوف", "حتى", "على", "من", "إلى")
    private val suffixes = listOf("هم", "كم", "كن", "نا", "ون", "ين", "ات", "ه", "ها", "ي", "ك", "ا")

    private const val MIN_PARTIAL_MATCH_LENGTH = 2

    /**
     * [strict] stops after the exact / phonetic-group / affix-strip tiers and skips the loose
     * partial-containment, edit-distance and soundex fallbacks entirely. Added after a real-device
     * finding: with a wide candidate window, those loose tiers matched 57 of 61 recognized tokens
     * even when the tokens were garbled ASR output, since short/common Arabic words repeat
     * constantly. [LocalCursorTracker] - which moves the cursor on *every* word, no
     * consecutive-agreement check - always calls with `strict = true`; a caller doing its own
     * multi-word confirmation before acting (e.g. a start-position lock) can afford the loose
     * default.
     */
    fun isMatch(spoken: String, expected: String, strict: Boolean = false): Boolean {
        val a = ArabicTextNormalizer.normalize(spoken)
        val b = ArabicTextNormalizer.normalize(expected)
        if (a.isEmpty() || b.isEmpty()) return false

        if (a == b) return true
        if (isPhoneticGroupMatch(a, b)) return true

        for (prefix in prefixes) {
            if (a.length > prefix.length && a.startsWith(prefix)) {
                val stripped = a.substring(prefix.length)
                if (stripped == b || isPhoneticGroupMatch(stripped, b)) return true
            }
        }
        for (suffix in suffixes) {
            if (a.length > suffix.length && a.endsWith(suffix)) {
                val stripped = a.substring(0, a.length - suffix.length)
                if (stripped == b || isPhoneticGroupMatch(stripped, b)) return true
            }
        }

        if (strict) return false

        // One side spoken/expected as a partial word (a short recitation cut off by the VAD, or
        // an elongation ASR split into two tokens).
        if (b.startsWith(a) && a.length >= MIN_PARTIAL_MATCH_LENGTH) return true
        if (a.startsWith(b) && b.length >= MIN_PARTIAL_MATCH_LENGTH) return true

        val distance = levenshteinDistance(a, b)
        val tolerance = maxOf(1, b.length / 3)
        if (distance <= tolerance) return true

        return arabicSoundex(a) == arabicSoundex(b) && arabicSoundex(a).isNotEmpty()
    }

    /** Same-length (or near-length, via [isPhoneticGroupMatchWithSkip]) char-by-char comparison
     * where letters in the same [phoneticGroups] entry count as equal. */
    private fun isPhoneticGroupMatch(a: String, b: String): Boolean {
        if (a.length != b.length) return isPhoneticGroupMatchWithSkip(a, b)
        for (i in a.indices) {
            if (!charsClose(a[i], b[i])) return false
        }
        return true
    }

    private fun charsClose(x: Char, y: Char): Boolean {
        if (x == y) return true
        val group = phoneticGroupOf[x] ?: return false
        return y in group
    }

    /** Allows up to 2 skipped characters on either side, for ASR output that dropped or
     * inserted a letter mid-word. */
    private fun isPhoneticGroupMatchWithSkip(a: String, b: String): Boolean {
        val maxSkip = 2
        if (kotlin.math.abs(a.length - b.length) > maxSkip) return false

        fun attempt(skipA: Int, skipB: Int): Boolean {
            var i = 0
            var j = 0
            var usedA = 0
            var usedB = 0
            while (i < a.length && j < b.length) {
                if (charsClose(a[i], b[j])) {
                    i++; j++
                    continue
                }
                if (usedA < skipA && i + 1 < a.length) {
                    i++; usedA++
                } else if (usedB < skipB && j + 1 < b.length) {
                    j++; usedB++
                } else {
                    return false
                }
            }
            return i == a.length && j == b.length
        }

        for (skipA in 0..maxSkip) {
            for (skipB in 0..maxSkip) {
                if (skipA == 0 && skipB == 0) continue
                if (attempt(skipA, skipB)) return true
            }
        }
        return false
    }

    private val soundexCodes: Map<Char, Char> = buildMap {
        for (ch in "حهخ") put(ch, '1')
        for (ch in "سصث") put(ch, '2')
        for (ch in "طتد") put(ch, '3')
        for (ch in "ظذض") put(ch, '4')
        for (ch in "عاأء غ".filterNot { it == ' ' }) put(ch, '5')
        for (ch in "قك") put(ch, '6')
        for (ch in "فبم") put(ch, '7')
        for (ch in "رز") put(ch, '8')
        for (ch in "يئى") put(ch, '9')
        for (ch in "وؤلن") put(ch, '0')
    }

    private fun arabicSoundex(text: String): String {
        if (text.isEmpty()) return ""
        val result = StringBuilder().append(text.first())
        var lastCode: Char? = null
        for (ch in text.drop(1)) {
            val code = soundexCodes[ch]
            if (code != null) {
                if (code != lastCode) result.append(code)
                lastCode = code
            } else {
                lastCode = null
            }
        }
        return result.take(4).toString().padEnd(4, '0')
    }

    fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + cost,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
