package com.iti.domain.repository

import com.iti.domain.model.ReadingProgress
import com.iti.domain.model.Sheikh
import com.iti.domain.model.StudyCircle
import com.iti.domain.model.User
import kotlinx.coroutines.flow.Flow


interface AlmahirRepository {

    fun observeCurrentUser(): Flow<User>

    fun observeReadingProgress(): Flow<ReadingProgress?>

    fun observeSheikhs(): Flow<List<Sheikh>>

    fun observeStudyCircles(): Flow<List<StudyCircle>>

    suspend fun joinStudyCircle(circleId: String)
}
