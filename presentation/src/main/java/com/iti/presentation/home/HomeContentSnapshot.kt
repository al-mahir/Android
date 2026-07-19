package com.iti.presentation.home

import com.iti.domain.model.ReadingProgress
import com.iti.domain.model.Sheikh
import com.iti.domain.model.StudyCircle
import com.iti.domain.model.User


internal data class HomeContentSnapshot(
    val user: User,
    val readingProgress: ReadingProgress?,
    val sheikhs: List<Sheikh>,
    val circles: List<StudyCircle>,
)
