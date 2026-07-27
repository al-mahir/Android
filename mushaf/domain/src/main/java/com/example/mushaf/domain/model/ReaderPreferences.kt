package com.example.mushaf.domain.model


data class ReaderPreferences(
    val tajweedEnabled: Boolean = true,
    val lastPage: Int = MushafConstants.FIRST_PAGE,
    val isFirstMushafLaunch: Boolean = true,
)

object MushafConstants {
    const val FIRST_PAGE = 1
    const val LAST_PAGE = 604
    const val LINES_PER_PAGE = 15

    fun clampPage(page: Int): Int = page.coerceIn(FIRST_PAGE, LAST_PAGE)

    /**
     * Starting page for each surah (1-indexed). SURAH_START_PAGES[0] = first page of Surah 1 (Al-Fatiha).
     * Based on the standard 604-page Hafs Mushaf.
     */
    val SURAH_START_PAGES = intArrayOf(
        1,   2,   50,  77,  106, 128, 151, 177, 187, 208,
        221, 235, 249, 255, 262, 267, 282, 293, 305, 312,
        322, 332, 342, 350, 359, 367, 377, 385, 396, 404,
        411, 415, 418, 428, 434, 440, 446, 453, 458, 467,
        477, 483, 489, 496, 499, 502, 507, 511, 515, 518,
        520, 523, 526, 528, 531, 534, 537, 542, 545, 549,
        551, 553, 554, 556, 558, 560, 562, 564, 566, 568,
        570, 572, 574, 575, 577, 578, 580, 582, 583, 585,
        586, 587, 587, 588, 589, 590, 591, 591, 592, 593,
        594, 595, 595, 596, 596, 597, 597, 598, 599, 599,
        600, 600, 601, 601, 601, 602, 602, 602, 603, 603,
        603, 604, 604, 604,
    )

    /** Returns the 1-based surah number that contains [page]. */
    fun surahForPage(page: Int): Int {
        val clamped = clampPage(page)
        // Binary search: find last surah whose start <= clamped
        var lo = 0; var hi = SURAH_START_PAGES.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (SURAH_START_PAGES[mid] <= clamped) lo = mid else hi = mid - 1
        }
        return lo + 1 // 1-based surah number
    }

    val JUZ_START_PAGES = intArrayOf(
        1,   22,  42,  62,  82,  102, 121, 142, 162, 182,
        201, 221, 242, 262, 282, 302, 322, 342, 362, 382,
        402, 422, 442, 462, 482, 502, 522, 542, 562, 582,
    )

    fun juzForPage(page: Int): Int {
        val clamped = clampPage(page)
        var lo = 0; var hi = JUZ_START_PAGES.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (JUZ_START_PAGES[mid] <= clamped) lo = mid else hi = mid - 1
        }
        return lo + 1
    }
}
