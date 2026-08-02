package com.iti.domain.usecase.bookmark

import com.iti.domain.core.Result
import com.iti.domain.core.getOrNull
import com.iti.domain.core.map
import com.iti.domain.model.Bookmark
import com.iti.domain.repository.AlmahirRepository

/**
 * Adds [bookmark] if its target isn't bookmarked yet, otherwise removes it. The target's
 * identity — and therefore its row id — is derived deterministically via [Bookmark.buildId],
 * ignoring whatever id the caller happened to set, so the same target always toggles the same
 * row instead of accumulating duplicates.
 */
class ToggleBookmarkUseCase(private val repository: AlmahirRepository) {
    suspend operator fun invoke(bookmark: Bookmark): Result<Boolean> {
        val id = Bookmark.buildId(
            type = bookmark.type,
            surahNumber = bookmark.surahNumber,
            ayahNumber = bookmark.ayahNumber,
            pageNumber = bookmark.pageNumber,
            sheikhId = bookmark.sheikhId,
        )
        val existing = repository.getBookmark(id).getOrNull()
        return if (existing != null) {
            repository.removeBookmark(id).map { false }
        } else {
            repository.addBookmark(bookmark.copy(id = id)).map { true }
        }
    }
}
