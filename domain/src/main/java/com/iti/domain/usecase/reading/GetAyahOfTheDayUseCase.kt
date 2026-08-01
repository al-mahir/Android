package com.iti.domain.usecase.reading

import com.iti.domain.model.AyahOfTheDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar


/**
 * Returns a deterministic [AyahOfTheDay] that changes once per day.
 *
 * Selection logic: picks an index in [AYAHS] using `dayOfYear % size`, so the displayed
 * ayah is stable for the whole day. No network or DB dependency — every consumer gets the
 * same result from the same seed.
 *
 * This is pure-domain logic (no framework APIs). A repository is intentionally not
 * involved; `AyahOfTheDay` is not persisted or synced — it is derived.
 */
class GetAyahOfTheDayUseCase {

    operator fun invoke(): Flow<AyahOfTheDay> = flow {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % AYAHS.size
        emit(AYAHS[index])
    }

    private companion object {

        /** Curated set of well-known ayahs (Arabic text with full tashkeel). */
        val AYAHS = listOf(
            AyahOfTheDay(
                arabicText = "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
                surahName = "الشرح",
                ayahNumber = 6,
                surahNumber = 94,
            ),
            AyahOfTheDay(
                arabicText = "وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ",
                surahName = "الطلاق",
                ayahNumber = 3,
                surahNumber = 65,
            ),
            AyahOfTheDay(
                arabicText = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا",
                surahName = "الشرح",
                ayahNumber = 5,
                surahNumber = 94,
            ),
            AyahOfTheDay(
                arabicText = "وَلَا تَيْأَسُوا مِن رَّوْحِ اللَّهِ",
                surahName = "يوسف",
                ayahNumber = 87,
                surahNumber = 12,
            ),
            AyahOfTheDay(
                arabicText = "أُولَٰئِكَ الَّذِينَ أَنْعَمَ اللَّهُ عَلَيْهِم مِّنَ النَّبِيِّينَ",
                surahName = "مريم",
                ayahNumber = 58,
                surahNumber = 19,
            ),
            AyahOfTheDay(
                arabicText = "وَاللَّهُ يُحِبُّ الصَّابِرِينَ",
                surahName = "آل عمران",
                ayahNumber = 146,
                surahNumber = 3,
            ),
            AyahOfTheDay(
                arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً",
                surahName = "البقرة",
                ayahNumber = 201,
                surahNumber = 2,
            ),
        )
    }
}
