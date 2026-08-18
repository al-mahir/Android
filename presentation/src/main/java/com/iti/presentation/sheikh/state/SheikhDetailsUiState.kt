package com.iti.presentation.sheikh.state

import com.iti.domain.model.Sheikh
import com.iti.meeting.domain.model.circle.Circle

data class SheikhDetailsUiState(
    val sheikh: Sheikh? = null,
    val circles: List<Circle> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
)
