package com.iti.data.mapper

import com.iti.data.dto.LegalDocumentDto
import com.iti.domain.model.LegalDocument
import com.iti.domain.model.LegalDocumentType

internal fun LegalDocumentType.toSlug(): String = when (this) {
    LegalDocumentType.ABOUT -> "about"
    LegalDocumentType.TERMS_OF_SERVICE -> "terms_of_service"
    LegalDocumentType.PRIVACY_POLICY -> "privacy_policy"
}

internal fun LegalDocumentDto.toDomain(): LegalDocument = LegalDocument(
    type = LegalDocumentType.entries.first { it.toSlug() == type.lowercase() },
    title = title,
    body = body,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
