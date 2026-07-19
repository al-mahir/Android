package com.iti.domain.repository

import com.iti.domain.model.LegalDocument
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.model.ReadingProgress
import com.iti.domain.model.Sheikh
import com.iti.domain.model.StudyCircle
import com.iti.domain.model.Subscription
import com.iti.domain.model.User
import kotlinx.coroutines.flow.Flow


interface AlmahirRepository {

    fun observeCurrentUser(): Flow<User>

    fun observeReadingProgress(): Flow<ReadingProgress?>

    fun observeSheikhs(): Flow<List<Sheikh>>

    fun observeStudyCircles(): Flow<List<StudyCircle>>

    fun observeSubscription(): Flow<Subscription>

    fun observeLegalDocument(type: LegalDocumentType): Flow<LegalDocument>

    suspend fun joinStudyCircle(circleId: String)


    suspend fun restorePurchases(): Boolean

    suspend fun logout()

    suspend fun deleteAccount()
}
