package com.iti.presentation.sheikh.state

import com.iti.domain.model.Sheikh
import com.iti.domain.model.StudyCircle

data class SheikhDetailsUiState(
    val sheikh: Sheikh? = null,
    val circles: List<StudyCircle> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
)
