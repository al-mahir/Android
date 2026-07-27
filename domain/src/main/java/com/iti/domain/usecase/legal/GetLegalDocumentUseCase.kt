package com.iti.domain.usecase.legal

import com.iti.domain.core.Result
import com.iti.domain.model.LegalDocument
import com.iti.domain.model.LegalDocumentType
import com.iti.domain.repository.AlmahirRepository
import kotlinx.coroutines.flow.Flow

class GetLegalDocumentUseCase(
    private val repository: AlmahirRepository,
) {
    operator fun invoke(type: LegalDocumentType): Flow<Result<LegalDocument>> =
        repository.observeLegalDocument(type)
}
