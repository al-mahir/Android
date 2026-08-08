package com.example.mushaf.domain.model.recite.local

/**
 * Resolves which word in a small lookahead window a settled, on-device-recognized word most
 * likely corresponds to. Bias toward [biasIndex] (the live cursor's last known position) so a
 * word that repeats inside the window — "من", "في" — resolves to the occurrence actually being
 * read rather than the nearest-to-zero one. Returns `null` on no match, which callers should
 * treat as "keep the previous position," never as "the reciter went silent."
 */
object LocalRecitationCursor {

    fun resolve(spokenWord: String, window: List<LocalWordEntry>, biasIndex: Int, strict: Boolean = false): Int? {
        var bestIndex: Int? = null
        var bestDistance = Int.MAX_VALUE
        window.forEachIndexed { index, entry ->
            if (!ArabicPhoneticMatcher.isMatch(spokenWord, entry.plainText, strict)) return@forEachIndexed
            val distance = kotlin.math.abs(index - biasIndex)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }
}
