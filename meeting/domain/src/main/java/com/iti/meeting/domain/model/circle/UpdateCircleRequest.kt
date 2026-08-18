package com.iti.meeting.domain.model.circle

/** Request body for `PATCH /api/circles/{circleId}` — all fields are optional;
 * only non-null fields should be sent to the server. */
data class UpdateCircleRequest(
    val name: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
)
