package com.iti.domain.model

data class Sheikh(
    val id: String,
    val name: String,
    val initials: String,
    val avatarUrl: String?,
    val rating: Double,
    val availability: SheikhAvailability,
)


enum class SheikhAvailability {
    AVAILABLE,
    IN_SESSION,
    OFFLINE,
}
