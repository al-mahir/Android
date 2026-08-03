package com.iti.meeting.domain.model.circle

/** A Qur'an study circle (group meeting) — see `docs/features/20-quran-study-circles-api.md`.
 * Timestamps are ISO-8601 strings; `currentMembers` excludes pending join requests. */
data class Circle(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String? = null,
    val type: CircleType = CircleType.PUBLIC,
    val status: CircleStatus = CircleStatus.SCHEDULED,
    val requiresApproval: Boolean = false,
    val maxParticipants: Int = 10,
    val currentMembers: Int = 0,
    val host: CircleMember? = null,
)
