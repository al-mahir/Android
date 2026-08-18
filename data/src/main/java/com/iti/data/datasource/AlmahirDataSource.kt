package com.iti.data.datasource

import com.iti.data.dto.LegalDocumentDto
import com.iti.data.dto.SubscriptionDto
import com.iti.data.dto.UserDto
import kotlinx.coroutines.flow.Flow


interface AlmahirDataSource {

    fun observeCurrentUser(): Flow<UserDto>

    fun observeSubscription(): Flow<SubscriptionDto>

    suspend fun selectSubscriptionPackage(packageId: String): SubscriptionDto

    fun observeLegalDocument(documentType: String): Flow<LegalDocumentDto>

    suspend fun requestSubscriptionCancellation(message: String)

    suspend fun logout()

    suspend fun deleteAccount()
}
