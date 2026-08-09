package com.example.mushaf.domain.model.recite.local

/**
 * One *pronunciation* unit of the reference text: the phonemes actually uttered, plus the word (or
 * words) it covers.
 *
 * Not always one word. Tajwīd routinely joins a word to the next one — `هُدًۭى لِّلْمُتَّقِينَ`
 * is a single unbroken `هُدَللِلمُتتَقِۦۦۦۦن` — so the reference phoneme table has fewer units than
 * the Mushaf has words for two thirds of all ayahs. Merges are always of *adjacent* words and the
 * table never splits one word into two units, which is what makes [PhonemeUnitAligner] able to
 * recover the mapping.
 *
 * [lastWordId] is what the highlight lands on when this unit is heard: the reciter has by then
 * finished every word in the span.
 */
data class ReferencePhonemeUnit(
    val phonemes: String,
    val wordIds: List<String>,
) {
    val lastWordId: String get() = wordIds.last()
}
