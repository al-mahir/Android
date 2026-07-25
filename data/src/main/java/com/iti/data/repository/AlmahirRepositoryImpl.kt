package com.iti.data.repository

import com.iti.data.datasource.AlmahirDataSource
import com.iti.data.mapper.toDomain
import com.iti.data.mapper.toSlug
import com.iti.domain.model.LegalDocument
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.model.ReadingProgress
import com.iti.domain.model.Sheikh
import com.iti.domain.model.StudyCircle
import com.iti.domain.model.Subscription
import com.iti.domain.model.User
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class AlmahirRepositoryImpl(
    private val dataSource: AlmahirDataSource,
) : AlmahirRepository {

    override fun observeCurrentUser(): Flow<User> =
        dataSource.observeCurrentUser().map { dto -> dto.toDomain() }

    override fun observeReadingProgress(): Flow<ReadingProgress?> =
        dataSource.observeReadingProgress().map { dto -> dto?.toDomain() }

    override fun observeSheikhs(): Flow<List<Sheikh>> =
        dataSource.observeSheikhs().map { dtos -> dtos.map { it.toDomain() } }

    override fun observeStudyCircles(): Flow<List<StudyCircle>> =
        dataSource.observeStudyCircles().map { dtos -> dtos.map { it.toDomain() } }

    override fun observeSubscription(): Flow<Subscription> =
        dataSource.observeSubscription().map { dto -> dto.toDomain() }

    override fun observeLegalDocument(type: LegalDocumentType): Flow<LegalDocument> =
        dataSource.observeLegalDocument(type.toSlug()).map { dto -> dto.toDomain() }

    override suspend fun joinStudyCircle(circleId: String) {
        dataSource.joinStudyCircle(circleId)
    }

    override suspend fun cancelJoinCircle(circleId: String) {
        dataSource.cancelJoinCircle(circleId)
    }

    override fun observeSheikhCircles(sheikhId: String): Flow<List<StudyCircle>> =
        dataSource.observeStudyCircles().map { dtos ->
            dtos.filter { it.hostId == sheikhId }.map { it.toDomain() }
        }

    override suspend fun restorePurchases(): Boolean = dataSource.restorePurchases()

    override suspend fun logout() {
        dataSource.logout()
    }

    override suspend fun deleteAccount() {
        dataSource.deleteAccount()
    }
}
