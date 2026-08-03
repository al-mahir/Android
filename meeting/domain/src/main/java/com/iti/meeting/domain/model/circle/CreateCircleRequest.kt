package com.iti.meeting.domain.model.circle

/** Input to `POST /api/circles`. PUBLIC circles may only be created by a Sheikh; PRIVATE circles
 * by any authenticated user. [password] is only used for PRIVATE circles. */
data class CreateCircleRequest(
    val name: String,
    val startDate: String,
    val endDate: String? = null,
    val type: CircleType = CircleType.PUBLIC,
    val requiresApproval: Boolean = false,
    val maxParticipants: Int = 10,
    val password: String? = null,
)
