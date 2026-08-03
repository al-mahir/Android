package com.iti.presentation.home

import com.iti.domain.model.AyahOfTheDay
import com.iti.domain.model.ReadingProgress
import com.iti.domain.model.User


internal data class HomeContentSnapshot(
    val user: User,
    val readingProgress: ReadingProgress?,
    val ayahOfTheDay: AyahOfTheDay?,
    val isOffline: Boolean,
)
