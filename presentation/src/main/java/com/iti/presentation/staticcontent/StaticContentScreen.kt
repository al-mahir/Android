package com.iti.presentation.staticcontent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iti.domain.model.LegalDocumentType
import com.iti.presentation.R
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.staticcontent.state.StaticContentEffect
import com.iti.presentation.staticcontent.state.StaticContentIntent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Composable
fun StaticContentScreen(
    documentType: LegalDocumentType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: StaticContentViewModel = koinViewModel(
        key = documentType.name,
        parameters = { parametersOf(documentType) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            StaticContentEffect.NavigateBack -> onBack()
        }
    }

    StaticContentContent(
        state = state,
        fallbackTitle = stringResource(documentType.fallbackTitleRes),
        onBackClick = { viewModel.onIntent(StaticContentIntent.BackClicked) },
        onRetryClick = { viewModel.onIntent(StaticContentIntent.Retry) },
        modifier = modifier,
    )
}

private val LegalDocumentType.fallbackTitleRes: Int
    get() = when (this) {
        LegalDocumentType.ABOUT -> R.string.profile_menu_about
        LegalDocumentType.TERMS_OF_SERVICE -> R.string.profile_menu_terms_of_service
        LegalDocumentType.PRIVACY_POLICY -> R.string.profile_menu_privacy_policy
    }
