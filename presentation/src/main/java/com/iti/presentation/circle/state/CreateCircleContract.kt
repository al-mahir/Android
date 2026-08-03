package com.iti.presentation.circle.state

import androidx.annotation.StringRes

// ── Create Circle MVI contract ──────────────────────────────────────────────

data class CreateCircleUiState(
    val title: String = "",
    val goals: String = "",
    val selectedType: CreateCirclePrivacyType = CreateCirclePrivacyType.PRIVATE,
    val password: String = "",
    val isCreating: Boolean = false,
    val titleError: Boolean = false,
    val passwordError: Boolean = false,
)

enum class CreateCirclePrivacyType { PRIVATE, PUBLIC }

sealed interface CreateCircleIntent {
    data class TitleChanged(val value: String) : CreateCircleIntent
    data class GoalsChanged(val value: String) : CreateCircleIntent
    data class PrivacySelected(val type: CreateCirclePrivacyType) : CreateCircleIntent
    data class PasswordChanged(val value: String) : CreateCircleIntent
    data object Submit : CreateCircleIntent
}

sealed interface CreateCircleEffect {
    data class CircleCreated(val circleId: String) : CreateCircleEffect
    data class ShowMessage(@StringRes val messageRes: Int) : CreateCircleEffect
}
