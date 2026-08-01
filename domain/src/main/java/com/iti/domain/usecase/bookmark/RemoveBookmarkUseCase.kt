package com.iti.domain.usecase.bookmark

import com.iti.domain.core.Result
import com.iti.domain.repository.AlmahirRepository

class RemoveBookmarkUseCase(private val repository: AlmahirRepository) {
    suspend operator fun invoke(id: String): Result<Unit> =
        repository.removeBookmark(id)
}
