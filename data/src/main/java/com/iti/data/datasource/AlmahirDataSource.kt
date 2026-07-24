package com.iti.data.datasource

import com.iti.data.dto.LegalDocumentDto
import com.iti.data.dto.ReadingProgressDto
import com.iti.data.dto.SheikhDto
import com.iti.data.dto.StudyCircleDto
import com.iti.data.dto.SubscriptionDto
import com.iti.data.dto.UserDto
import kotlinx.coroutines.flow.Flow


interface AlmahirDataSource {

    fun observeCurrentUser(): Flow<UserDto>

    fun observeReadingProgress(): Flow<ReadingProgressDto?>

    fun observeSheikhs(): Flow<List<SheikhDto>>

    fun observeStudyCircles(): Flow<List<StudyCircleDto>>

    fun observeSubscription(): Flow<SubscriptionDto>

    fun observeLegalDocument(documentType: String): Flow<LegalDocumentDto>

    suspend fun joinStudyCircle(circleId: String)

    suspend fun cancelJoinCircle(circleId: String)

    suspend fun restorePurchases(): Boolean

    suspend fun logout()

    suspend fun deleteAccount()
}
