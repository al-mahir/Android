package com.iti.domain.model


data class LegalDocument(
    val type: LegalDocumentType,
    val title: String,
    val body: String,
    val updatedAtEpochMillis: Long?,
)

enum class LegalDocumentType {
    ABOUT,
    TERMS_OF_SERVICE,
    PRIVACY_POLICY,
}
