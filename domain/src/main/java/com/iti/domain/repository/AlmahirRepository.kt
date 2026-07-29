package com.iti.domain.repository

import com.iti.domain.core.Result
import com.iti.domain.model.LegalDocument
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.model.Subscription
import com.iti.domain.model.User
import kotlinx.coroutines.flow.Flow


interface AlmahirRepository {

    fun observeCurrentUser(): Flow<Result<User>>

    fun observeSubscription(): Flow<Result<Subscription>>

    fun observeLegalDocument(type: LegalDocumentType): Flow<Result<LegalDocument>>

    suspend fun restorePurchases(): Result<Boolean>

    suspend fun logout(): Result<Unit>

    suspend fun deleteAccount(): Result<Unit>
}
