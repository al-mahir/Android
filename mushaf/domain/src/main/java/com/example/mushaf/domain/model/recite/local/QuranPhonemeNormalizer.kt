package com.example.mushaf.domain.model.recite.local

/**
 * Folds the phoneme alphabet `Muno459/zipformer_p-quran` emits — and the Uthmani script the
 * reference words are written in — down to one comparable form.
 *
 * The model's vocabulary is 251 symbols of bare Arabic letters and letter+ḥaraka pairs, with **no
 * space and no word-boundary marker**, so its output is one unbroken phoneme string. Length in
 * that string carries madd/ghunna duration (`ررَحِۦۦۦۦم`) and gemination (`ررَحمَان`), which is a
 * tajwīd-grading signal, not a position signal — grading stays server-owned, so runs are collapsed
 * here rather than compared. What's left is a compact skeleton where the hypothesis and the
 * reference can be lined up character by character.
 */
object QuranPhonemeNormalizer {

    /** Small waw/ya (madd carriers) and noon ghunna are letters as far as position tracking goes. */
    private const val SMALL_WAW = 'ۥ'
    private const val SMALL_YA = 'ۦ'
    private const val NOON_GHUNNA = 'ں'

    private val charFolds: Map<Char, Char?> = buildMap {
        put(SMALL_WAW, 'و')
        put(SMALL_YA, 'ي')
        put(NOON_GHUNNA, 'ن')

        // Hamza is the least reliably transcribed sound in the corpus (the same wasl alif surfaces
        // as 'ء', as 'ا', or as nothing at all depending on where the phonetizer joined it), so it
        // is dropped on both sides rather than compared.
        put('ء', null)
        put('ڇ', null) // qalqala echo
        put('۾', null)
        put('ـ', null) // tatweel

        for (alif in "أإآٱىٰٲ") put(alif, 'ا')
        put('ؤ', 'و')
        put('ئ', 'ي')
        put('ة', 'ه')
    }

    private const val HARAKA_START = 'ً'
    private const val HARAKA_END = 'ْ'
    private const val QURANIC_MARK_START = 'ۖ'
    private const val QURANIC_MARK_END = 'ۭ'

    /**
     * The form both sides of a live comparison use: ḥarakāt kept (they are real signal — the model
     * predicts them and they separate otherwise-identical skeletons), repeated characters collapsed.
     */
    fun normalize(text: String): String = fold(text, keepHarakat = true)

    /**
     * The form used to line phoneme units up against plain Uthmani words. Ḥarakāt are dropped here
     * because the two sides spell them differently (sukūn and shadda have no phoneme symbol at
     * all — gemination is written as a doubled letter, which the run-collapse then folds away).
     */
    fun skeleton(text: String): String = fold(text, keepHarakat = false)

    private fun fold(text: String, keepHarakat: Boolean): String {
        val builder = StringBuilder(text.length)
        for (ch in text) {
            val folded = when {
                charFolds.containsKey(ch) -> charFolds[ch] ?: continue
                ch in HARAKA_START..HARAKA_END -> if (keepHarakat && ch.isSpokenHaraka()) ch else continue
                ch in QURANIC_MARK_START..QURANIC_MARK_END -> continue
                ch.isWhitespace() -> continue
                else -> ch
            }
            if (builder.isNotEmpty() && builder.last() == folded) continue
            builder.append(folded)
        }
        return builder.toString()
    }

    /** Fatḥa/ḍamma/kasra are the only ḥarakāt in the model's vocabulary; sukūn, shadda and the
     * tanwīn marks never appear in its output, so keeping them on the reference side only would
     * make every reference longer than every hypothesis. */
    private fun Char.isSpokenHaraka(): Boolean = this == 'َ' || this == 'ُ' || this == 'ِ'
}
