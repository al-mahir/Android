package com.example.mushaf.domain.model.recite.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicPhoneticMatcherTest {

    @Test
    fun `hamza spelling variants match after normalization alone`() {
        assertTrue(ArabicPhoneticMatcher.isMatch("أحمد", "احمد"))
        assertTrue(ArabicPhoneticMatcher.isMatch("احمد", "أحمد"))
    }

    @Test
    fun `letters in the same phonetic group substitute`() {
        assertTrue(ArabicPhoneticMatcher.isMatch("سراط", "صراط"))
    }

    @Test
    fun `a leading waw prefix is stripped before comparing`() {
        assertTrue(ArabicPhoneticMatcher.isMatch("والحمد", "الحمد"))
    }

    @Test
    fun `a trailing pronoun suffix is stripped before comparing`() {
        assertTrue(ArabicPhoneticMatcher.isMatch("كتابها", "كتاب"))
    }

    @Test
    fun `a short cut-off recitation partially matching the expected word still matches`() {
        assertTrue(ArabicPhoneticMatcher.isMatch("بس", "بسم"))
    }

    @Test
    fun `one extra inserted letter still matches`() {
        assertTrue(ArabicPhoneticMatcher.isMatch("الرحمن", "الرحمان"))
    }

    @Test
    fun `a single substituted unrelated letter is within levenshtein tolerance`() {
        // "الرحيم" vs "الرحين" - final letter unrelated (م/ن aren't in the same phonetic
        // group), so this only matches via the edit-distance fallback, not group substitution.
        assertTrue(ArabicPhoneticMatcher.isMatch("الرحين", "الرحيم"))
    }

    @Test
    fun `unrelated words do not match`() {
        // Same length, no shared phonetic group at any position, so this can't be rescued by
        // the skip-tolerant fallback that same-length comparisons don't use.
        assertFalse(ArabicPhoneticMatcher.isMatch("قلم", "بيت"))
        // Very different lengths - outside both the skip tolerance and the levenshtein budget.
        assertFalse(ArabicPhoneticMatcher.isMatch("قلم", "المستقيم"))
    }

    @Test
    fun `empty input never matches`() {
        assertFalse(ArabicPhoneticMatcher.isMatch("", "احمد"))
        assertFalse(ArabicPhoneticMatcher.isMatch("احمد", ""))
    }

    @Test
    fun `strict mode still allows the exact, phonetic-group and affix tiers`() {
        assertTrue(ArabicPhoneticMatcher.isMatch("أحمد", "احمد", strict = true))
        assertTrue(ArabicPhoneticMatcher.isMatch("سراط", "صراط", strict = true))
        assertTrue(ArabicPhoneticMatcher.isMatch("والحمد", "الحمد", strict = true))
        assertTrue(ArabicPhoneticMatcher.isMatch("كتابها", "كتاب", strict = true))
    }

    @Test
    fun `strict mode rejects matches that only the loose fallback tiers would allow`() {
        // Only reachable via partial-containment in loose mode - "بسم" is one character longer
        // than "بس" but that doesn't trigger the phonetic-group skip tolerance (no mismatched
        // character to trigger a skip; it just runs out of "بس" first), so this is genuinely
        // loose-only.
        assertFalse(ArabicPhoneticMatcher.isMatch("بس", "بسم", strict = true))
        // Only reachable via edit-distance in loose mode - "ن" and "م" aren't in the same
        // phonetic group, so same-length comparison can't rescue this one either.
        assertFalse(ArabicPhoneticMatcher.isMatch("الرحين", "الرحيم", strict = true))
    }
}
