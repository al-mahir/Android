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
                ReciterDto(
                    id = 7,
                    name = "Mishary Rashid Alafasy",
                    nameArabic = "مشاري راشد العفاسي",
                    style = "Murattal",
                    audioBaseUrl = "https://audio.qurancdn.com/Alafasy/mp3/"
                )
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
                segments = mappedSegments
            )
        }
        android.util.Log.d("RecitationRemoteDataSource", "Mapped ${timings.size} timings for page $pageNumber")
        emit(timings)
    }
}
