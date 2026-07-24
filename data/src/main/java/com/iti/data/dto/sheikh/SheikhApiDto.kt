package com.iti.data.dto.sheikh

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO representing a Sheikh as returned by the real backend API.
 * Field names match the actual API response keys.
 *
 * The API returns either:
 *  - GET /api/sheikh      → ApiResponse<List<SheikhApiDto>>
 *  - GET /api/sheikh/{id} → ApiResponse<SheikhApiDto>
 */
@Serializable
data class SheikhApiDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String? = null,
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("phoneNumber") val phoneNumber: String? = null,
    @SerialName("profilePictureUrl") val profilePictureUrl: String? = null,
    @SerialName("sheikhStatus") val sheikhStatus: String? = null,
    // Computed / derived fields that may be present
    @SerialName("name") val name: String? = null,
    @SerialName("rating") val rating: Double? = null,
    @SerialName("reviewCount") val reviewCount: Int? = null,
    @SerialName("specialization") val specialization: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("activeCircleCount") val activeCircleCount: Int? = null,
    @SerialName("totalStudents") val totalStudents: Int? = null,
)
