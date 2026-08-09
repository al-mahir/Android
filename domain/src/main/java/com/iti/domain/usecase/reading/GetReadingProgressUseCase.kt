package com.iti.domain.usecase.reading

import com.iti.domain.model.ReadingProgress
import com.iti.domain.model.quran.SurahNames
import com.iti.domain.repository.ReadingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class GetReadingProgressUseCase(
    private val repository: ReadingProgressRepository,
) {
    operator fun invoke(): Flow<ReadingProgress?> = repository.observeLastPage().map { page ->
        val surahNumber = surahForPage(page)
        val surah = SURAHS.getOrNull(surahNumber - 1) ?: return@map null
        
        val startPage = SURAH_START_PAGES[surahNumber - 1]
        val endPage = if (surahNumber < 114) {
            SURAH_START_PAGES[surahNumber]
        } else 604
        val totalPagesInSurah = maxOf(1, endPage - startPage + 1)
        val pagesReadInSurah = maxOf(1, page - startPage + 1)
        val fraction = pagesReadInSurah.toFloat() / totalPagesInSurah
        val estimatedAyah = (fraction * surah.verseCount).toInt().coerceIn(1, surah.verseCount)

        ReadingProgress(
            surahNumber = surahNumber,
            surahName = surah.nameEn,
            ayahNumber = estimatedAyah,              // Estimated based on page progress
            pageNumber = page,
            juzNumber = juzForPage(page),
            surahTotalAyahs = surah.verseCount,
            surahReadAyahs = estimatedAyah,          // Drives the UI progress bar percentage
        )
    }

    // ── Static lookup tables (Hafs 604-page Mushaf) ──────────────────────────

    /** Starting page for each surah; index 0 = Surah 1 (Al-Fatiha). */
    private val SURAH_START_PAGES = intArrayOf(
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

    /** Starting page for each juz (30 juz); index 0 = Juz 1. */
    private val JUZ_START_PAGES = intArrayOf(
        1,   22,  42,  62,  82,  102, 121, 142, 162, 182,
        201, 221, 242, 262, 282, 302, 322, 342, 362, 382,
        402, 422, 442, 462, 482, 502, 522, 542, 562, 582,
    )

    private fun surahForPage(page: Int): Int {
        val clamped = page.coerceIn(1, 604)
        var lo = 0; var hi = SURAH_START_PAGES.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (SURAH_START_PAGES[mid] <= clamped) lo = mid else hi = mid - 1
        }
        return lo + 1
    }

    private fun juzForPage(page: Int): Int {
        val clamped = page.coerceIn(1, 604)
        var lo = 0; var hi = JUZ_START_PAGES.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (JUZ_START_PAGES[mid] <= clamped) lo = mid else hi = mid - 1
        }
        return lo + 1
    }

    /** Minimal surah metadata for the domain-layer lookup. */
    private data class SurahMeta(val nameEn: String, val verseCount: Int)

    private val SURAHS = listOf(
        SurahMeta("Al-Fatihah", 7),    SurahMeta("Al-Baqarah", 286),  SurahMeta("Ali 'Imran", 200),
        SurahMeta("An-Nisa", 176),     SurahMeta("Al-Ma'idah", 120),  SurahMeta("Al-An'am", 165),
        SurahMeta("Al-A'raf", 206),    SurahMeta("Al-Anfal", 75),     SurahMeta("At-Tawbah", 129),
        SurahMeta("Yunus", 109),       SurahMeta("Hud", 123),         SurahMeta("Yusuf", 111),
        SurahMeta("Ar-Ra'd", 43),      SurahMeta("Ibrahim", 52),      SurahMeta("Al-Hijr", 99),
        SurahMeta("An-Nahl", 128),     SurahMeta("Al-Isra", 111),     SurahMeta("Al-Kahf", 110),
        SurahMeta("Maryam", 98),       SurahMeta("Ta-Ha", 135),       SurahMeta("Al-Anbiya", 112),
        SurahMeta("Al-Hajj", 78),      SurahMeta("Al-Mu'minun", 118), SurahMeta("An-Nur", 64),
        SurahMeta("Al-Furqan", 77),    SurahMeta("Ash-Shu'ara", 227), SurahMeta("An-Naml", 93),
        SurahMeta("Al-Qasas", 88),     SurahMeta("Al-'Ankabut", 69),  SurahMeta("Ar-Rum", 60),
        SurahMeta("Luqman", 34),       SurahMeta("As-Sajdah", 30),    SurahMeta("Al-Ahzab", 73),
        SurahMeta("Saba", 54),         SurahMeta("Fatir", 45),        SurahMeta("Ya-Sin", 83),
        SurahMeta("As-Saffat", 182),   SurahMeta("Sad", 88),          SurahMeta("Az-Zumar", 75),
        SurahMeta("Ghafir", 85),       SurahMeta("Fussilat", 54),     SurahMeta("Ash-Shura", 53),
        SurahMeta("Az-Zukhruf", 89),   SurahMeta("Ad-Dukhan", 59),    SurahMeta("Al-Jathiyah", 37),
        SurahMeta("Al-Ahqaf", 35),     SurahMeta("Muhammad", 38),     SurahMeta("Al-Fath", 29),
        SurahMeta("Al-Hujurat", 18),   SurahMeta("Qaf", 45),          SurahMeta("Adh-Dhariyat", 60),
        SurahMeta("At-Tur", 49),       SurahMeta("An-Najm", 62),      SurahMeta("Al-Qamar", 55),
        SurahMeta("Ar-Rahman", 78),    SurahMeta("Al-Waqi'ah", 96),   SurahMeta("Al-Hadid", 29),
        SurahMeta("Al-Mujadila", 22),  SurahMeta("Al-Hashr", 24),     SurahMeta("Al-Mumtahanah", 13),
        SurahMeta("As-Saf", 14),       SurahMeta("Al-Jumu'ah", 11),   SurahMeta("Al-Munafiqun", 11),
        SurahMeta("At-Taghabun", 18),  SurahMeta("At-Talaq", 12),     SurahMeta("At-Tahrim", 12),
        SurahMeta("Al-Mulk", 30),      SurahMeta("Al-Qalam", 52),     SurahMeta("Al-Haqqah", 52),
        SurahMeta("Al-Ma'arij", 44),   SurahMeta("Nuh", 28),          SurahMeta("Al-Jinn", 28),
        SurahMeta("Al-Muzzammil", 20), SurahMeta("Al-Muddaththir", 56),SurahMeta("Al-Qiyamah", 40),
        SurahMeta("Al-Insan", 31),     SurahMeta("Al-Mursalat", 50),  SurahMeta("An-Naba", 40),
        SurahMeta("An-Nazi'at", 46),   SurahMeta("'Abasa", 42),       SurahMeta("At-Takwir", 29),
        SurahMeta("Al-Infitar", 19),   SurahMeta("Al-Mutaffifin", 36),SurahMeta("Al-Inshiqaq", 25),
        SurahMeta("Al-Buruj", 22),     SurahMeta("At-Tariq", 17),     SurahMeta("Al-A'la", 19),
        SurahMeta("Al-Ghashiyah", 26), SurahMeta("Al-Fajr", 30),      SurahMeta("Al-Balad", 20),
        SurahMeta("Ash-Shams", 15),    SurahMeta("Al-Layl", 21),      SurahMeta("Ad-Duha", 11),
        SurahMeta("Ash-Sharh", 8),     SurahMeta("At-Tin", 8),        SurahMeta("Al-'Alaq", 19),
        SurahMeta("Al-Qadr", 5),       SurahMeta("Al-Bayyinah", 8),   SurahMeta("Az-Zalzalah", 8),
        SurahMeta("Al-'Adiyat", 11),   SurahMeta("Al-Qari'ah", 11),   SurahMeta("At-Takathur", 8),
        SurahMeta("Al-'Asr", 3),       SurahMeta("Al-Humazah", 9),    SurahMeta("Al-Fil", 5),
        SurahMeta("Quraysh", 4),       SurahMeta("Al-Ma'un", 7),      SurahMeta("Al-Kawthar", 3),
        SurahMeta("Al-Kafirun", 6),    SurahMeta("An-Nasr", 3),       SurahMeta("Al-Masad", 5),
        SurahMeta("Al-Ikhlas", 4),     SurahMeta("Al-Falaq", 5),      SurahMeta("An-Nas", 6),
    )
}
