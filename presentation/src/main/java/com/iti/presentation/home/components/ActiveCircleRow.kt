package com.iti.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.theme.Theme
import com.iti.domain.model.StudyCircle
import com.iti.presentation.R

private val JoinButtonMinWidth = 84.dp


@Composable
fun ActiveCircleRow(
    circle: StudyCircle,
    isJoining: Boolean,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(Theme.colors.surfaceContainer)
            .border(width = 1.dp, color = Theme.colors.surfaceVariant, shape = Theme.shapes.large)
            .padding(Theme.spacing.medium),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
            modifier = Modifier.weight(1f),
        ) {
            BasicText(
                text = circle.surahName,
                style = Theme.typography.body.large.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = circle.hostName,
                style = Theme.typography.body.small.copy(color = Theme.colors.secondaryFont),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        PrimaryButton(
            caption = stringResource(
                if (circle.isJoined) R.string.home_joined else R.string.home_join,
            ),
            onClick = onJoinClick,
            isLoading = isJoining,
            isDisabled = circle.isJoined,
            height = 36.dp,
            shape = CircleShape,
            captionStyle = Theme.typography.body.medium,
            modifier = Modifier.widthIn(min = JoinButtonMinWidth),
        )
    }
}
