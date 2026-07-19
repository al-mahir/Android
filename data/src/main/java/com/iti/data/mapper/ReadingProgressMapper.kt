package com.iti.data.mapper

import com.iti.data.dto.ReadingProgressDto
import com.iti.domain.model.ReadingProgress

internal fun ReadingProgressDto.toDomain(): ReadingProgress = ReadingProgress(
    surahName = surahName,
    ayahNumber = ayahNumber,
    pageNumber = pageNumber,
)
