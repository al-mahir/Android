package com.example.mushaf.domain.model


data class ReaderPreferences(
    val tajweedEnabled: Boolean = true,
    val lastPage: Int = MushafConstants.FIRST_PAGE,
    val isFirstMushafLaunch: Boolean = true,
    val downloadOverWifiOnly: Boolean = false,
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

    /**
     * Standard Hafs Mushaf hizb-quarter boundary pages (240 entries).
     * Index 0 = start of hizb-quarter 1, index 239 = start of hizb-quarter 240.
     */
    val HIZB_QUARTER_START_PAGES = intArrayOf(
        1,   4,   7,  10,  13,  16,  19,  22,  24,  26,
       28,  30,  32,  34,  36,  38,  40,  42,  44,  46,
       48,  50,  53,  56,  59,  62,  64,  66,  68,  70,
       72,  74,  76,  78,  80,  82,  84,  86,  88,  90,
       92,  94,  96,  98, 100, 102, 104, 106, 108, 110,
      112, 114, 116, 118, 120, 121, 122, 124, 126, 128,
      130, 132, 134, 136, 138, 140, 142, 144, 146, 148,
      150, 151, 152, 154, 156, 158, 160, 162, 164, 166,
      168, 170, 172, 174, 176, 177, 178, 180, 182, 184,
      186, 187, 188, 190, 192, 194, 196, 198, 200, 201,
      202, 204, 206, 208, 210, 212, 214, 216, 218, 220,
      221, 222, 224, 226, 228, 230, 232, 234, 236, 238,
      240, 242, 244, 246, 248, 250, 252, 254, 256, 258,
      260, 262, 264, 266, 268, 270, 272, 274, 276, 278,
      280, 282, 284, 286, 288, 290, 292, 294, 296, 298,
      300, 302, 304, 306, 308, 310, 312, 314, 316, 318,
      320, 322, 324, 326, 328, 330, 332, 334, 336, 338,
      340, 342, 344, 346, 348, 350, 352, 354, 356, 358,
      360, 362, 364, 366, 368, 370, 372, 374, 376, 377,
      378, 380, 382, 384, 386, 388, 390, 392, 394, 396,
      398, 400, 402, 404, 406, 408, 410, 412, 414, 416,
      418, 420, 422, 424, 426, 428, 430, 432, 434, 436,
      438, 440, 442, 444, 446, 448, 450, 452, 454, 456,
      458, 460, 462, 464, 466, 468, 470, 472, 474, 476,
    )

    /**
     * Returns the 1-based hizb-quarter index (1..240) for the given page.
     * The quarter within the current hizb is ((result - 1) % 4) + 1.
     */
    fun hizbQuarterForPage(page: Int): Int {
        val clamped = clampPage(page)
        var lo = 0; var hi = HIZB_QUARTER_START_PAGES.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (HIZB_QUARTER_START_PAGES[mid] <= clamped) lo = mid else hi = mid - 1
        }
        return lo + 1
    }
}
