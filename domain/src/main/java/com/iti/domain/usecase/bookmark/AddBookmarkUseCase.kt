package com.iti.domain.usecase.bookmark

import com.iti.domain.core.Result
import com.iti.domain.model.Bookmark
import com.iti.domain.repository.AlmahirRepository

class AddBookmarkUseCase(private val repository: AlmahirRepository) {
    suspend operator fun invoke(bookmark: Bookmark): Result<Unit> =
        repository.addBookmark(bookmark)
}
