package com.iti.domain.usecase.bookmark

import com.iti.domain.core.Result
import com.iti.domain.model.Bookmark
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow

class ObserveAllBookmarksUseCase(private val repository: AlmahirRepository) {
    operator fun invoke(): Flow<Result<List<Bookmark>>> = repository.observeAllBookmarks()
}
