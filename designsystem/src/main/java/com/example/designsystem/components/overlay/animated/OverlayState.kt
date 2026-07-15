package com.example.designsystem.components.overlay.animated

/**
 * Visible status communicated to the user. Use `null` (not a state) to mean
 * "no overlay shown" — that keeps consumers from having to enumerate a
 * placeholder branch in `when` blocks.
 */
sealed interface OverlayState {
    data object Loading : OverlayState
    data class Success(val message: String? = null) : OverlayState
    data class Error(val message: String? = null) : OverlayState
}
