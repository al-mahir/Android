package com.example.mushaf.data.mapper

/**
 * Resolves the per-page font codepoint used to render a word (research R2).
 *
 * QPC v4 uses one glyph-font per page (`p{n}.ttf`) where each word is a single glyph. Each
 * page font maps its word glyphs contiguously starting at [PUA_BASE], in page reading order,
 * so a word's codepoint is `PUA_BASE + (wordId - pageFirstWordId)`.
 *
 * VERIFIED (task T050): confirmed against the shipped fonts' `cmap` — the word-glyph block
 * begins at U+FC41 on every page (e.g. page 1's 36 words map to U+FC41..U+FC64), and this
 * formula yields an in-font glyph for every word across all 604 pages. This object is the
 * single point of change should the encoding ever differ.
 */
object GlyphCodeResolver {

    private const val PUA_BASE = 0xFC41

    /**
     * @param pageFirstWordId the smallest word id present on the page (the page's first word).
     * @param wordId the global word id being rendered.
     * @return the codepoint into the page font.
     */
    fun resolve(pageFirstWordId: Int, wordId: Int): Int =
        PUA_BASE + (wordId - pageFirstWordId)
}
