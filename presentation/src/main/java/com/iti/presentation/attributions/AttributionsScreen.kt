package com.iti.presentation.attributions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.core.platform.ExternalActions

/**
 * Credits for the content the app ships.
 *
 * **This screen is a licence obligation, not a courtesy.** The Qur'an text comes from the Tanzil
 * Project under CC BY 3.0, which requires any app shipping it to credit the project with a link
 * to <https://tanzil.net>.
 *
 * It is deliberately local and unconditional — hardcoded strings rather than a document fetched
 * like the terms and privacy pages. An attribution that depends on a server returning the right
 * markdown is an attribution that can silently disappear, and the obligation binds the app
 * whether or not a backend answers.
 */
@Composable
fun AttributionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tanzilUrl = stringResource(R.string.attribution_tanzil_url)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.attributions_title),
            onBackClick = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Theme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        ) {
            AttributionCard(
                title = stringResource(R.string.attribution_quran_title),
                body = stringResource(R.string.attribution_quran_body),
                linkLabel = tanzilUrl,
                onLinkClick = { ExternalActions.openUrl(context, tanzilUrl) },
            )

            AttributionCard(
                title = stringResource(R.string.attribution_mushaf_title),
                body = stringResource(R.string.attribution_mushaf_body),
            )

            AttributionCard(
                title = stringResource(R.string.attribution_models_title),
                body = stringResource(R.string.attribution_models_body),
            )
        }
    }
}

@Composable
private fun AttributionCard(
    title: String,
    body: String,
    linkLabel: String? = null,
    onLinkClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.medium)
            .border(1.dp, Theme.colors.border, Theme.shapes.medium)
            .background(Theme.colors.surface)
            .padding(Theme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        BasicText(
            text = title,
            style = Theme.typography.body.large.copy(
                color = Theme.colors.primaryFont,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = body,
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
        )
        if (linkLabel != null && onLinkClick != null) {
            BasicText(
                text = linkLabel,
                style = Theme.typography.body.medium.copy(
                    color = Theme.colors.primary,
                    textDecoration = TextDecoration.Underline,
                ),
                modifier = Modifier.clickable(onClick = onLinkClick),
            )
        }
    }
}
