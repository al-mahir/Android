package com.iti.domain.repository

import com.iti.domain.core.Result
import com.iti.domain.model.Bookmark
import com.iti.domain.model.BookmarkType
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

    fun observeBookmarks(type: BookmarkType): Flow<Result<List<Bookmark>>>

    fun observeAllBookmarks(): Flow<Result<List<Bookmark>>>

    suspend fun getBookmarks(type: BookmarkType): Result<List<Bookmark>>

    suspend fun getBookmark(id: String): Result<Bookmark?>

    suspend fun addBookmark(bookmark: Bookmark): Result<Unit>

    suspend fun removeBookmark(id: String): Result<Unit>
}
