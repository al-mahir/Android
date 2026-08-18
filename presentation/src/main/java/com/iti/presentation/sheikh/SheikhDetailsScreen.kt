package com.iti.presentation.sheikh

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.loading.shimmer
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.rating.RatingLabel
import com.example.designsystem.components.section.SectionHeader
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.domain.model.Sheikh
import com.iti.meeting.domain.model.circle.Circle
import com.iti.presentation.R
import com.iti.presentation.circle.CircleCard
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.sheikh.state.SheikhDetailsEffect
import com.iti.presentation.sheikh.state.SheikhDetailsIntent
import com.iti.presentation.sheikh.state.SheikhDetailsUiState
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Room left under the scrolling body so the pinned meeting CTA never covers content — the bar
 * is a button plus its padding, and it carries the navigation-bar inset on gesture devices. */
private val BodyBottomInset = 120.dp

@Composable
fun SheikhDetailsScreen(
    sheikhId: String,
    onBack: () -> Unit,
    onOpenCircle: (String) -> Unit,
    onRequestMeeting: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SheikhDetailsViewModel = koinViewModel(
        key = sheikhId,
        parameters = { parametersOf(sheikhId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            SheikhDetailsEffect.NavigateBack -> onBack()
            is SheikhDetailsEffect.OpenCircle -> onOpenCircle(effect.circleId)
        }
    }

    SheikhDetailsContent(
        state = state,
        onBack = onBack,
        onCircleClick = { viewModel.onIntent(SheikhDetailsIntent.CircleClicked(it)) },
        onRetry = { viewModel.onIntent(SheikhDetailsIntent.Retry) },
        onRequestMeeting = onRequestMeeting,
        modifier = modifier,
    )
}

@Composable
private fun SheikhDetailsContent(
    state: SheikhDetailsUiState,
    onBack: () -> Unit,
    onCircleClick: (String) -> Unit,
    onRetry: () -> Unit,
    onRequestMeeting: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = stringResource(R.string.sheikh_details_title),
            onBackClick = onBack,
        )

        when {
            state.isLoading -> SheikhDetailsSkeleton(modifier = Modifier.weight(1f))
            state.isError || state.sheikh == null -> NetworkErrorScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                onRetry = onRetry,
            )
            else -> {
                val sheikh = state.sheikh
                Box(modifier = Modifier.weight(1f)) {
                    SheikhDetailsBody(
                        sheikh = sheikh,
                        circles = state.circles,
                        onCircleClick = onCircleClick,
                    )
                    // The one action this screen exists for stays reachable at any scroll depth.
                    RequestMeetingBar(
                        onClick = { onRequestMeeting(sheikh.id, sheikh.name) },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun SheikhDetailsBody(
    sheikh: Sheikh,
    circles: List<Circle>,
    onCircleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Theme.spacing.medium,
            end = Theme.spacing.medium,
            top = Theme.spacing.medium,
            bottom = BodyBottomInset,
        ),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        item(key = "profile") { SheikhProfileCard(sheikh = sheikh) }

        item(key = "bio") {
            SheikhSectionCard(title = stringResource(R.string.sheikh_section_bio)) {
                BasicText(
                    text = sheikh.bio.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.sheikh_details_about_empty),
                    style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
                )
            }
        }

        item(key = "circles-header") {
            SectionHeader(title = stringResource(R.string.sheikh_section_active_circles))
        }

        if (circles.isEmpty()) {
            item(key = "circles-empty") {
                BasicText(
                    text = stringResource(R.string.sheikh_details_no_circles),
                    style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                )
            }
        } else {
            items(circles, key = { it.id }) { circle ->
                CircleCard(
                    circle = circle,
                    onClick = { onCircleClick(circle.id) },
                )
            }
        }
    }
}

/** Identity block: avatar, name, specialization, availability and the headline numbers. */
@Composable
private fun SheikhProfileCard(
    sheikh: Sheikh,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(Theme.colors.surface)
            .border(width = 1.dp, color = Theme.colors.surfaceVariant, shape = Theme.shapes.large)
            .padding(Theme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        SheikhInitialsAvatar(
            initials = sheikh.initials,
            sheikhId = sheikh.id,
            size = 88,
            avatarUrl = sheikh.avatarUrl,
            contentDescription = stringResource(R.string.sheikh_cd_avatar, sheikh.name),
        )

        BasicText(
            text = sheikh.name,
            style = Theme.typography.title.copy(
                color = Theme.colors.primaryFont,
                textAlign = TextAlign.Center,
            ),
        )

        sheikh.specialization.takeIf { it.isNotBlank() }?.let { specialization ->
            BasicText(
                text = specialization,
                style = Theme.typography.body.medium.copy(
                    color = Theme.colors.secondaryFont,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        SheikhStatusChip(availability = sheikh.availability)

        Spacer(modifier = Modifier.height(Theme.spacing.small))
        HorizontalDivider(thickness = 1.dp, color = Theme.colors.surfaceVariant)
        Spacer(modifier = Modifier.height(Theme.spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SheikhStat(
                label = stringResource(R.string.sheikh_stat_rating),
                modifier = Modifier.weight(1f),
            ) {
                RatingLabel(
                    rating = sheikh.rating,
                    contentDescription = stringResource(R.string.sheikh_stat_rating),
                    textColor = Theme.colors.primaryFont,
                )
            }
            StatSeparator()
            SheikhStat(
                label = stringResource(R.string.sheikh_stat_reviews),
                value = sheikh.reviewCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatSeparator()
            SheikhStat(
                label = stringResource(R.string.sheikh_stat_students),
                value = sheikh.totalStudents.toString(),
                modifier = Modifier.weight(1f),
            )
            StatSeparator()
            SheikhStat(
                label = stringResource(R.string.sheikh_stat_circles),
                value = sheikh.activeCircleCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatSeparator() {
    VerticalDivider(
        thickness = 1.dp,
        color = Theme.colors.surfaceVariant,
        modifier = Modifier.height(28.dp),
    )
}

@Composable
private fun SheikhStat(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    valueSlot: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (value != null) {
            BasicText(
                text = value,
                style = Theme.typography.body.large.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        } else {
            valueSlot()
        }
        BasicText(
            text = label,
            style = Theme.typography.body.small.copy(
                color = Theme.colors.secondaryFont,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun SheikhSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(Theme.colors.surface)
            .border(width = 1.dp, color = Theme.colors.surfaceVariant, shape = Theme.shapes.large)
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
        content()
    }
}

/** Pinned action bar holding the screen's primary call to action. */
@Composable
private fun RequestMeetingBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround),
    ) {
        HorizontalDivider(thickness = 1.dp, color = Theme.colors.surfaceVariant)
        PrimaryButton(
            caption = stringResource(R.string.sheikh_details_request_meeting),
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun SheikhDetailsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Theme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(Theme.shapes.large)
                .shimmer(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(Theme.shapes.large)
                .shimmer(),
        )
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(Theme.shapes.large)
                    .shimmer(),
            )
        }
    }
}
