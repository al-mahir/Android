package com.example.mushaf.presentation.core.error

import com.example.designsystem.text.UiText
import com.example.mushaf.presentation.R
import com.iti.domain.core.DomainError

/**
 * Maps a [DomainError] from anywhere in the Mushaf data/domain layers to a localized [UiText],
 * so ViewModels never need an Android `Context` to build user-facing error text.
 */
fun DomainError.toUiText(): UiText = when (this) {
    is DomainError.NetworkError -> UiText.Resource(R.string.error_network)
    is DomainError.ServerError -> message.toUiTextOrGeneric()
    is DomainError.ValidationError -> message.toUiTextOrGeneric()
    is DomainError.ConflictError -> message.toUiTextOrGeneric()
    is DomainError.Unauthorized -> UiText.Resource(R.string.error_unauthorized)
    is DomainError.NotFound -> UiText.Resource(R.string.error_not_found)
    is DomainError.Unknown -> UiText.Resource(R.string.error_generic)
}

private fun String?.toUiTextOrGeneric(): UiText =
    if (isNullOrBlank()) UiText.Resource(R.string.error_generic) else UiText.Dynamic(this)
