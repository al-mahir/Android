package com.iti.data.datasource

import com.iti.data.local.bookmark.BookmarkDao
import com.iti.data.local.bookmark.BookmarkEntity
import kotlinx.coroutines.flow.Flow


interface AlmahirLocalDataSource {
    fun observeBookmarksByType(type: String): Flow<List<BookmarkEntity>>

    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    suspend fun getBookmarksByType(type: String): List<BookmarkEntity>

    suspend fun getBookmarkById(id: String): BookmarkEntity?

    suspend fun upsertBookmark(bookmark: BookmarkEntity)

    suspend fun deleteBookmark(id: String)
}

class AlmahirLocalDataSourceImpl(
    private val bookmarkDao: BookmarkDao,
) : AlmahirLocalDataSource {

    override fun observeBookmarksByType(type: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.observeByType(type)

    override fun observeAllBookmarks(): Flow<List<BookmarkEntity>> =
        bookmarkDao.observeAll()

    override suspend fun getBookmarksByType(type: String): List<BookmarkEntity> =
        bookmarkDao.getByType(type)

    override suspend fun getBookmarkById(id: String): BookmarkEntity? =
        bookmarkDao.getById(id)

    override suspend fun upsertBookmark(bookmark: BookmarkEntity) =
        bookmarkDao.insert(bookmark)

    override suspend fun deleteBookmark(id: String) =
        bookmarkDao.delete(id)
}
