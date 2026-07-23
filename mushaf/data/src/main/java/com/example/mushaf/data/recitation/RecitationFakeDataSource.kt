package com.example.mushaf.data.recitation

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

class RecitationFakeDataSource : RecitationDataSource {

    override fun observeReciters(): Flow<List<ReciterDto>> = flow {
        emit(SEED_RECITERS)
    }.onStart { delay(800) } 

    override fun observeTimingsForPage(reciterId: Int, pageNumber: Int): Flow<List<AyahTimingDto>> = flow {
        if (pageNumber == 1) {
            emit(SEED_FATIHA_TIMINGS)
        } else {
            
            emit(emptyList()) 
        }
    }.onStart { delay(500) }

    companion object {
        val SEED_RECITERS = listOf(
            ReciterDto(
                id = 7,
                name = "Mishary Rashid Alafasy",
                nameArabic = "مشاري راشد العفاسي",
                style = "Murattal",
                audioBaseUrl = "https://audio.qurancdn.com/Alafasy/mp3/"
            ),
            ReciterDto(
                id = 2,
                name = "AbdulBaset AbdulSamad",
                nameArabic = "عبد الباسط عبد الصمد",
                style = "Mujawwad",
                audioBaseUrl = "https://audio.qurancdn.com/Abdul_Basit_Mujawwad_128kbps/"
            ),
            ReciterDto(
                id = 1,
                name = "Mahmoud Khalil Al-Husary",
                nameArabic = "محمود خليل الحصري",
                style = "Murattal",
                audioBaseUrl = "https://audio.qurancdn.com/Husary_128kbps/"
            )
        )

        val SEED_FATIHA_TIMINGS = listOf(
            AyahTimingDto(
                verseKey = "1:1",
                timestampFrom = 0,
                timestampTo = 6493,
                segments = listOf(
                    listOf(0, 0, 150),
                    listOf(1, 150, 400),
                    listOf(2, 400, 850),
                    listOf(3, 850, 1200)
                )
            ),
            AyahTimingDto(
                verseKey = "1:2",
                timestampFrom = 6493,
                timestampTo = 12000,
                segments = listOf(
                    listOf(0, 6493, 7000),
                    listOf(1, 7000, 7500),
                    listOf(2, 7500, 8000),
                    listOf(3, 8000, 9000)
                )
            )
        )
    }
}
