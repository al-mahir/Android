package com.example.mushaf.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.DriverManager

/**
 * Guards the bundled `mushaf_v4_layout.db` against the glyph-misalignment class of bug.
 *
 * The QPC-V4 page fonts encode every page's tokens as one contiguous Private-Use block starting at
 * U+FC41 (word glyphs and interleaved pause/waqf-mark glyphs alike). The layout DB is correct for a
 * page iff concatenating that page's word glyph strings, in line then position order, reproduces
 * exactly `FC41, FC42, … FC41+N-1` with no gap, repeat, or early stop. A gap/short sequence is what
 * made lines end early and dropped trailing ayah markers. (Per-page equality with the actual font
 * glyph counts was verified at DB build time across all 604 pages.)
 */
class MushafLayoutDbIntegrityTest {

    private val pua0 = 0xFC41

    private fun dbFile(): File {
        // Unit tests run with the module dir as the working directory.
        val candidates = listOf(
            "src/main/assets/databases/mushaf_v4_layout.db",
            "mushaf/data/src/main/assets/databases/mushaf_v4_layout.db",
        )
        return candidates.map(::File).firstOrNull { it.exists() }
            ?: error("layout DB asset not found (cwd=${File(".").absolutePath})")
    }

    @Test
    fun `every page's word glyphs form a contiguous FC41 block`() {
        DriverManager.getConnection("jdbc:sqlite:${dbFile().absolutePath}").use { con ->
            val pageCount = con.createStatement().use { st ->
                st.executeQuery("SELECT number_of_pages FROM info").use { it.next(); it.getInt(1) }
            }
            assertEquals(604, pageCount)

            var totalGlyphs = 0
            con.prepareStatement(
                "SELECT glyph_text FROM words WHERE page_number = ? ORDER BY line_number ASC, position ASC",
            ).use { ps ->
                for (page in 1..pageCount) {
                    val codes = ArrayList<Int>()
                    ps.setInt(1, page)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val glyphs = rs.getString(1) ?: ""
                            var i = 0
                            while (i < glyphs.length) {
                                val cp = glyphs.codePointAt(i)
                                codes += cp
                                i += Character.charCount(cp)
                            }
                        }
                    }
                    totalGlyphs += codes.size
                    assertTrue("page $page has no glyphs", codes.isNotEmpty())
                    val expected = (0 until codes.size).map { pua0 + it }
                    assertEquals(
                        "page $page glyph codes are not a contiguous FC41.. block " +
                            "(first=${codes.firstOrNull()?.toString(16)}, last=${codes.lastOrNull()?.toString(16)}, n=${codes.size})",
                        expected,
                        codes,
                    )
                }
            }

            val wordRows = con.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM words").use { rs -> rs.next(); rs.getInt(1) }
            }
            // QPC-V4 word+ayah-marker token count (KFGQPC 1441H layout #19).
            assertEquals(83668, wordRows)
            // Total page-font glyphs across all 604 pages (words + interleaved mark glyphs).
            assertEquals(88186, totalGlyphs)
        }
    }
}
