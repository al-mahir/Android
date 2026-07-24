package com.example.mushaf.domain.model.recite.local

import java.text.Normalizer

/**
 * Folds spelling variants that are pronounced identically down to one canonical form, so text
 * from an ASR engine (modern spelling, no tashkīl) can be compared against Qur'an corpus text
 * without every hamza/alif variant being treated as a different word. Standard Arabic-search
 * normalization (diacritic/tatweel stripping, alif/hamza unification, alif maqṣūra -> yā') —
 * not Qur'an-specific.
 */
object ArabicTextNormalizer {

    private val charFolds: Map<Char, String> = buildMap {
        // Hamza carriers -> bare alif; hamza itself drops (it isn't a separate letter to an ASR
        // engine transcribing modern spelling).
        put('إ', "ا")
        put('أ', "ا")
        put('آ', "ا")
        put('ٱ', "ا")
        put('ء', "")
        put('ئ', "ي")
        put('ؤ', "و")

        put('ة', "ه")
        put('ى', "ي")
        put('ـ', "") // tatweel

        // Tashkīl / Qur'anic annotation marks.
        for (mark in "َُِّْٰٓٔٗٙٚٛۖۗۘۙۚۛ۞۩ًٌٍ") put(mark, "")

        put('ﷲ', "الله")
        put('ﷻ', "الله")
    }

    private val whitespaceRun = Regex("\\s+")

    fun normalize(text: String): String {
        // NFKC first: composes decomposed hamza sequences some IMEs/ASR engines emit, so the
        // char-fold table below (built from precomposed codepoints) actually matches them.
        val composed = Normalizer.normalize(text, Normalizer.Form.NFKC)
        val builder = StringBuilder(composed.length)
        for (ch in composed) {
            val fold = charFolds[ch]
            if (fold != null) builder.append(fold) else builder.append(ch)
        }
        return builder.toString()
            .trim()
            .replace(whitespaceRun, " ")
    }
}
