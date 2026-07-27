package com.iti.data.datasource

import com.iti.data.dto.LegalDocumentDto
import com.iti.data.dto.SubscriptionDto
import com.iti.data.dto.UserDto
import kotlinx.coroutines.flow.Flow


interface AlmahirDataSource {

    fun observeCurrentUser(): Flow<UserDto>

    fun observeSubscription(): Flow<SubscriptionDto>

    fun observeLegalDocument(documentType: String): Flow<LegalDocumentDto>

    suspend fun restorePurchases(): Boolean

    suspend fun logout()

    suspend fun deleteAccount()
}
