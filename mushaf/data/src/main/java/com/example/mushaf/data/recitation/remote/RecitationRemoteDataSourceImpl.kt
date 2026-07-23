package com.example.mushaf.data.recitation.remote

import com.example.mushaf.data.recitation.AyahTimingDto
import com.example.mushaf.data.recitation.RecitationDataSource
import com.example.mushaf.data.recitation.ReciterDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RecitationRemoteDataSourceImpl(
    private val quranApi: QuranApi
) : RecitationDataSource {

    override fun observeReciters(): Flow<List<ReciterDto>> = flow {

        emit(
            listOf(
                ReciterDto(7, "Mishari Rashid al-`Afasy", "مشاري راشد العفاسي", "Murattal", "https://audio.qurancdn.com/Alafasy/mp3/"),
                ReciterDto(3, "Abdur-Rahman as-Sudais", "عبدالرحمن السديس", "Murattal", "https://audio.qurancdn.com/Sudais/mp3/"),
                ReciterDto(4, "Abu Bakr al-Shatri", "أبو بكر الشاطري", "Murattal", "https://audio.qurancdn.com/Shatri/mp3/"),
                ReciterDto(5, "Hani ar-Rifai", "هاني الرفاعي", "Murattal", "https://audio.qurancdn.com/Rifai/mp3/"),
                ReciterDto(1, "AbdulBaset AbdulSamad", "عبدالباسط عبدالصمد (مجود)", "Mujawwad", "https://audio.qurancdn.com/AbdulBaset/Mujawwad/mp3/"),
                ReciterDto(2, "AbdulBaset AbdulSamad", "عبدالباسط عبدالصمد (مرتل)", "Murattal", "https://audio.qurancdn.com/AbdulBaset/Murattal/mp3/"),
                ReciterDto(6, "Mahmoud Khalil Al-Husary", "محمود خليل الحصري", "Murattal", "https://audio.qurancdn.com/Husary/mp3/"),
                ReciterDto(12, "Mahmoud Khalil Al-Husary", "محمود خليل الحصري (معلم)", "Muallim", "https://audio.qurancdn.com/Husary/Muallim/mp3/"),
                ReciterDto(9, "Mohamed Siddiq al-Minshawi", "محمد صديق المنشاوي (مرتل)", "Murattal", "https://audio.qurancdn.com/Minshawy/Murattal/mp3/"),
                ReciterDto(8, "Mohamed Siddiq al-Minshawi", "محمد صديق المنشاوي (مجود)", "Mujawwad", "https://audio.qurancdn.com/Minshawy/Mujawwad/mp3/"),
                ReciterDto(10, "Sa`ud ash-Shuraym", "سعود الشريم", "Murattal", "https://audio.qurancdn.com/Shuraym/mp3/"),
                ReciterDto(11, "Mohamed al-Tablawi", "محمد الطبلاوي", "Murattal", "https://audio.qurancdn.com/Tablawi/mp3/")
            )
        )
    }

    override fun observeTimingsForPage(
        reciterId: Int,
        pageNumber: Int
    ): Flow<List<AyahTimingDto>> = flow {
        android.util.Log.d("RecitationRemoteDataSource", "Fetching timings for page $pageNumber, reciter $reciterId")
        val response = quranApi.getVersesByPage(pageNumber, reciterId)
        android.util.Log.d("RecitationRemoteDataSource", "Received ${response.verses.size} verses for page $pageNumber")
        
        val timings = response.verses.mapNotNull { verse ->
            val audio = verse.audio ?: return@mapNotNull null
            
            
            
            val mappedSegments = audio.segments.map { rawSegment ->
                if (rawSegment.size >= 4) {
                    
                    listOf(rawSegment[0], rawSegment[2], rawSegment[3])
                } else if (rawSegment.size == 3) {
                    
                    listOf(rawSegment[0], rawSegment[1], rawSegment[2])
                } else {
                    emptyList()
                }
            }.filter { it.isNotEmpty() }

            AyahTimingDto(
                verseKey = verse.verseKey,
                timestampFrom = mappedSegments.firstOrNull()?.get(1) ?: 0L,
                timestampTo = mappedSegments.lastOrNull()?.get(2) ?: 0L,
                audioUrl = audio.url,
                segments = mappedSegments
            )
        }
        android.util.Log.d("RecitationRemoteDataSource", "Mapped ${timings.size} timings for page $pageNumber")
        emit(timings)
    }
}
