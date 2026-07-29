package com.iti.presentation.meetingrequest.browse

import com.iti.domain.model.MeetingSheikhSummary

data class SheikhBrowseUiState(
    val isLoading: Boolean = false,
    val sheikhs: List<MeetingSheikhSummary> = emptyList(),
    val errorMessage: String? = null,
)



