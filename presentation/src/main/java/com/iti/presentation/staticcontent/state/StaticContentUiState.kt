package com.iti.presentation.staticcontent.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.iti.domain.model.LegalDocument

@Immutable
data class StaticContentUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
    val document: LegalDocument? = null,
) {
    val hasError: Boolean get() = errorMessageRes != null && document == null
}
