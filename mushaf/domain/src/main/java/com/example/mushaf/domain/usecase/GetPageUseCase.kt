package com.example.mushaf.domain.usecase

import com.example.mushaf.domain.model.MushafConstants
import com.example.mushaf.domain.model.MushafPage
import com.example.mushaf.domain.repository.MushafRepository
import com.iti.domain.core.Result
import kotlinx.coroutines.flow.Flow

class GetPageUseCase(
    private val repository: MushafRepository,
) {
    operator fun invoke(pageNumber: Int): Flow<Result<MushafPage>> =
        repository.getPage(MushafConstants.clampPage(pageNumber))
}
