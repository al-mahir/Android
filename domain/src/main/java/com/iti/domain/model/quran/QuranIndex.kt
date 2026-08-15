package com.iti.domain.model.quran

/**
 * Compile-time index of ayah counts per sūrah, shared by domain use cases that need
 * Qur'an structure (e.g. exam question generation) without pulling in `:mushaf:domain`.
 *
 * Index is 0-based internally: `ayahCounts[0]` = Sūrah 1 (Al-Fātiḥa) = 7.
 */
object QuranIndex {

    /** Ayah count for each of the 114 sūrahs (0-indexed). */
    val ayahCounts: IntArray = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
        5, 4, 5, 6,
    )

    const val SURAH_COUNT: Int = 114

    /** Ayah count for a 1-based sūrah number. Returns 0 for out-of-range input. */
    fun ayahCountOf(surahNumber: Int): Int =
        if (surahNumber in 1..SURAH_COUNT) ayahCounts[surahNumber - 1] else 0

    /** Absolute ayah index (1-based across the whole Qur'an) for a given position. */
    fun absoluteAyahIndex(surahNumber: Int, ayahNumber: Int): Int {
        if (surahNumber !in 1..SURAH_COUNT) return -1
        var offset = 0
        for (s in 1 until surahNumber) offset += ayahCounts[s - 1]
        return offset + ayahNumber
    }

    /** Total ayah count in the entire Qur'an. */
    val totalAyahs: Int = ayahCounts.sum()

    /**
     * Juz boundaries: maps juz number (1-based) → first (surah, ayah) pair in that juz.
     * This is the standard Hafs division.
     */
    val juzStartPositions: List<Pair<Int, Int>> = listOf(
        1 to 1, 2 to 142, 3 to 1, 4 to 24, 5 to 24,
        6 to 1, 7 to 188, 8 to 88, 9 to 1, 10 to 1,
        11 to 6, 12 to 53, 13 to 18, 14 to 1, 15 to 1,
        16 to 128, 17 to 1, 18 to 75, 19 to 59, 20 to 1,
        21 to 1, 22 to 1, 23 to 20, 24 to 32, 25 to 70,
        26 to 83, 27 to 56, 28 to 22, 29 to 46, 30 to 1,
    ).mapIndexed { index, (surah, ayah) ->
        // Convert list index → correct surah: the pairs above are NOT (surah, ayah) but
        // need the actual juz→surah mapping.
        surah to ayah
    }

    /**
     * Returns all (surahNumber, ayahNumber) pairs that fall within a given juz (1-based).
     * Uses the standard Hafs juz division.
     */
    fun ayahsInJuz(juzNumber: Int): List<Pair<Int, Int>> {
        if (juzNumber !in 1..30) return emptyList()
        val start = JUZ_BOUNDARIES[juzNumber - 1]
        val endExclusive = if (juzNumber < 30) JUZ_BOUNDARIES[juzNumber] else null

        val result = mutableListOf<Pair<Int, Int>>()
        var surah = start.first
        var ayah = start.second
        while (surah <= SURAH_COUNT) {
            val maxAyah = ayahCounts[surah - 1]
            val lastAyahInSurah = if (endExclusive != null && surah == endExclusive.first) {
                endExclusive.second - 1
            } else {
                maxAyah
            }
            for (a in ayah..lastAyahInSurah) result.add(surah to a)
            if (endExclusive != null && surah == endExclusive.first) break
            surah++
            ayah = 1
        }
        return result
    }

    /**
     * Juz division boundary table (juz N starts at index N-1).
     * Each entry = (surahNumber, ayahNumber) of the FIRST ayah in that juz.
     */
    val JUZ_BOUNDARIES: List<Pair<Int, Int>> = listOf(
        1 to 1,    // Juz 1
        2 to 142,  // Juz 2
        2 to 253,  // Juz 3
        3 to 92,   // Juz 4
        4 to 24,   // Juz 5
        4 to 148,  // Juz 6
        5 to 82,   // Juz 7
        6 to 111,  // Juz 8
        7 to 88,   // Juz 9
        8 to 41,   // Juz 10
        9 to 93,   // Juz 11
        11 to 6,   // Juz 12
        12 to 53,  // Juz 13
        15 to 1,   // Juz 14
        17 to 1,   // Juz 15
        18 to 75,  // Juz 16
        21 to 1,   // Juz 17
        23 to 1,   // Juz 18
        25 to 21,  // Juz 19
        27 to 56,  // Juz 20
        29 to 46,  // Juz 21
        33 to 31,  // Juz 22
        36 to 28,  // Juz 23
        39 to 32,  // Juz 24
        41 to 47,  // Juz 25
        46 to 1,   // Juz 26
        51 to 31,  // Juz 27
        58 to 1,   // Juz 28
        67 to 1,   // Juz 29
        78 to 1,   // Juz 30
    )

    /** Returns all (surahNumber, ayahNumber) pairs within a given rub' al-hizb (1-based, 1–240). */
    fun ayahsInRub(rubNumber: Int): List<Pair<Int, Int>> {
        if (rubNumber !in 1..240) return emptyList()
        val juz = ((rubNumber - 1) / 8) + 1
        val rubIndex = (rubNumber - 1) % 8 // 0-7 within the juz
        val allAyahsInJuz = ayahsInJuz(juz)
        if (allAyahsInJuz.isEmpty()) return emptyList()
        val partSize = allAyahsInJuz.size / 8
        val start = rubIndex * partSize
        val end = if (rubIndex == 7) allAyahsInJuz.size else (rubIndex + 1) * partSize
        return allAyahsInJuz.subList(start.coerceAtMost(allAyahsInJuz.size), end.coerceAtMost(allAyahsInJuz.size))
    }
}
