package com.iti.domain.model

data class Sheikh(
    val id: String,
    val name: String,
    val initials: String,
    val avatarUrl: String?,
    val rating: Double,
    val availability: SheikhAvailability,
    val reviewCount: Int = 0,
    val specialization: String = "",
    val bio: String = "",
    val activeCircleCount: Int = 0,
    val totalStudents: Int = 0,
)

enum class SheikhAvailability {
    AVAILABLE,
    IN_SESSION,
    OFFLINE,
}
