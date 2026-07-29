package com.iti.presentation.sheikh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.components.placeholderscreens.NetworkErrorScreen
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.theme.Theme
import com.iti.domain.model.Sheikh
import com.iti.domain.model.StudyCircle
import com.iti.presentation.R
import com.iti.presentation.circle.CircleCard
import com.iti.presentation.core.mvi.ObserveEffect
import com.iti.presentation.sheikh.state.SheikhDetailsEffect
import com.iti.presentation.sheikh.state.SheikhDetailsIntent
import com.iti.presentation.sheikh.state.SheikhDetailsUiState
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SheikhDetailsScreen(
    sheikhId: String,
    onBack: () -> Unit,
    onNavigateToJoiningCircle: (String) -> Unit,
    onRequestMeeting: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SheikhDetailsViewModel = koinViewModel(parameters = { parametersOf(sheikhId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEffect(viewModel.effect) { effect ->
        when (effect) {
            SheikhDetailsEffect.NavigateBack -> onBack()
            is SheikhDetailsEffect.NavigateToJoiningCircle ->
                onNavigateToJoiningCircle(effect.circleId)
        }
    }

    SheikhDetailsContent(
        state = state,
        onBack = onBack,
        onJoinCircle = { viewModel.onIntent(SheikhDetailsIntent.JoinCircle(it)) },
        onRetry = { viewModel.onIntent(SheikhDetailsIntent.Retry) },
        onRequestMeeting = onRequestMeeting,
        modifier = modifier,
    )
}

@Composable
private fun SheikhDetailsContent(
    state: SheikhDetailsUiState,
    onBack: () -> Unit,
    onJoinCircle: (String) -> Unit,
    onRetry: () -> Unit,
    onRequestMeeting: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.backGround),
    ) {
        BackTitleTopBar(
            title = state.sheikh?.name ?: stringResource(R.string.sheikh_details_title),
            onBackClick = onBack,
        )

        when {
            state.isLoading -> SheikhDetailsSkeleton()
            state.isError || state.sheikh == null -> NetworkErrorScreen(
                modifier = Modifier.fillMaxSize(),
                onRetry = onRetry,
            )
            else -> SheikhDetailsBody(
                sheikh = state.sheikh,
                circles = state.circles,
                onJoinCircle = onJoinCircle,
                onRequestMeeting = { onRequestMeeting(state.sheikh.id) },
            )
        }
    }
}

@Composable
private fun SheikhDetailsBody(
    sheikh: Sheikh,
    circles: List<StudyCircle>,
    onJoinCircle: (String) -> Unit,
    onRequestMeeting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SheikhInitialsAvatar(
                initials = sheikh.initials,
                sheikhId = sheikh.id,
                size = 96,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = sheikh.name,
                style = Theme.typography.title,
                color = Theme.colors.primaryFont,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sheikh.specialization,
                style = Theme.typography.body.medium,
                color = Theme.colors.secondaryFont,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SheikhStatusChip(availability = sheikh.availability)
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                caption = stringResource(R.string.sheikh_details_request_meeting),
                onClick = onRequestMeeting,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SheikhStat(label = stringResource(R.string.sheikh_stat_rating), value = sheikh.rating.toString())
            SheikhStat(label = stringResource(R.string.sheikh_stat_reviews), value = sheikh.reviewCount.toString())
            SheikhStat(label = stringResource(R.string.sheikh_stat_students), value = sheikh.totalStudents.toString())
            SheikhStat(label = stringResource(R.string.sheikh_stat_circles), value = sheikh.activeCircleCount.toString())
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Theme.colors.surface, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        if (sheikh.bio.isNotBlank()) {
            Text(
                text = stringResource(R.string.sheikh_section_bio),
                style = Theme.typography.body.large,
                color = Theme.colors.primaryFont,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = sheikh.bio,
                style = Theme.typography.body.medium,
                color = Theme.colors.secondaryFont,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (circles.isNotEmpty()) {
            Text(
                text = stringResource(R.string.sheikh_section_active_circles),
                style = Theme.typography.body.large,
                color = Theme.colors.primaryFont,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            circles.forEach { circle ->
                CircleCard(
                    circle = circle,
                    onJoin = { onJoinCircle(circle.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SheikhStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = Theme.typography.body.large, color = Theme.colors.primary)
        Text(text = label, style = Theme.typography.body.small, color = Theme.colors.secondaryFont)
    }
}

@Composable
private fun SheikhDetailsSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)).background(Theme.colors.surface))
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.width(160.dp).height(20.dp).clip(RoundedCornerShape(8.dp)).background(Theme.colors.surface))
        Spacer(modifier = Modifier.height(48.dp))
        repeat(3) {
            Box(modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(16.dp)).background(Theme.colors.surface))
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
