package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.TafsirBook
import com.example.mushaf.domain.repository.MushafRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveAvailableTafsirBooksUseCase(
    private val repository: MushafRepository,
) {
    operator fun invoke(): Flow<List<TafsirBook>> {
        // Repository guarantees mukhtasar is always at index 0.
        // We sort only the tail (downloadable books) alphabetically by display name.
        return repository.observeAvailableTafsirBooks().map { books ->
            val mukhtasar = books.filter { it.tafsirKey == "mukhtasar" }
            val rest = books.filter { it.tafsirKey != "mukhtasar" }.sortedBy { it.displayName }
            mukhtasar + rest
        }
    }
}
