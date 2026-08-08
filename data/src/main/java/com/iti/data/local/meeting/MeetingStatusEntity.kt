package com.iti.data.local.meeting

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meeting_status")
data class MeetingStatusEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val meetingTime: Long,
    val status: String
)
