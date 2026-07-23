package com.example.mushaf.data.recitation

import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.RecitationStyle
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.domain.model.WordTiming

object RecitationMapper {

    fun toDomain(dto: ReciterDto): Reciter {
        val style = when (dto.style.lowercase()) {
            "mujawwad" -> RecitationStyle.MUJAWWAD
            "muallim" -> RecitationStyle.MUALLIM
            else -> RecitationStyle.MURATTAL
        }
        return Reciter(
            id = dto.id,
            name = dto.name,
            nameArabic = dto.nameArabic,
            style = style,
            audioBaseUrl = dto.audioBaseUrl
        )
    }

    fun toDomain(dto: AyahTimingDto): AyahTiming {
        val (surah, ayah) = dto.verseKey.split(":").map { it.toInt() }
        
        val wordTimings = dto.segments.map { segmentArray ->
            
            WordTiming(
                wordIndex = segmentArray[0].toInt(),
                startMs = segmentArray[1],
                endMs = segmentArray[2]
            )
        }

        return AyahTiming(
            surahNumber = surah,
            ayahNumber = ayah,
            timestampFrom = dto.timestampFrom,
            timestampTo = dto.timestampTo,
            audioUrl = dto.audioUrl,
            wordTimings = wordTimings
        )
    }
}
