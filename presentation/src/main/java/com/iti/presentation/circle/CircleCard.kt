package com.iti.presentation.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.theme.Theme
import com.iti.domain.model.CircleDifficulty
import com.iti.domain.model.StudyCircle
import com.iti.presentation.R
import com.iti.presentation.sheikh.SheikhInitialsAvatar

@Composable
fun CircleCard(
    circle: StudyCircle,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.colors.surface)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = circle.surahName,
                style = Theme.typography.body.large,
                color = Theme.colors.onSurface,
            )
            if (circle.isLive) LiveBadge()
        }

        Spacer(modifier = Modifier.height(6.dp))
        DifficultyBadge(difficulty = circle.difficulty)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SheikhInitialsAvatar(
                initials = circle.hostInitials,
                sheikhId = circle.hostId,
                size = 36,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circle.hostName,
                    style = Theme.typography.body.medium,
                    color = Theme.colors.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.circle_participants_label,
                        circle.participantCount,
                        circle.maxParticipants,
                        circle.currentActivity,
                    ),
                    style = Theme.typography.body.small,
                    color = Theme.colors.secondaryFont,
                )
            }

            if (!circle.isJoined && !circle.isWaitingApproval) {
                PrimaryButton(
                    caption = stringResource(R.string.circle_join),
                    onClick = onJoin,
                    height = ButtonHeightCompact,
                    modifier = Modifier.width(80.dp),
                )
            } else if (circle.isWaitingApproval) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Theme.colors.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.circle_waiting),
                        style = Theme.typography.body.small,
                        color = Theme.colors.secondaryFont,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFFEBEE))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFE53935)))
        Text(text = "LIVE", style = Theme.typography.body.small, color = Color(0xFFE53935))
    }
}

@Composable
private fun DifficultyBadge(difficulty: CircleDifficulty) {
    val (label, bg, fg) = when (difficulty) {
        CircleDifficulty.BEGINNER -> Triple(
            stringResource(R.string.circle_difficulty_beginner),
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
        )
        CircleDifficulty.INTERMEDIATE -> Triple(
            stringResource(R.string.circle_difficulty_intermediate),
            Color(0xFFFFF8E1),
            Color(0xFFF57F17),
        )
        CircleDifficulty.ADVANCED -> Triple(
            stringResource(R.string.circle_difficulty_advanced),
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
        )
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = label, style = Theme.typography.body.small, color = fg)
    }
}
