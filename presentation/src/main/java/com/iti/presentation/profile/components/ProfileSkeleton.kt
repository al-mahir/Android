package com.iti.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.loading.ShimmerBox
import com.example.designsystem.theme.Theme

@Composable
internal fun ProfileSkeleton(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.large),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBox(Modifier.size(Theme.size.avatarMedium).clip(CircleShape))
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                ShimmerBox(Modifier.width(140.dp).height(18.dp).clip(Theme.shapes.small))
                ShimmerBox(Modifier.width(180.dp).height(14.dp).clip(Theme.shapes.small))
            }
        }

        ShimmerBox(Modifier.fillMaxWidth().height(40.dp).clip(Theme.shapes.medium))
        ShimmerBox(Modifier.fillMaxWidth().height(56.dp).clip(Theme.shapes.large))
        ShimmerBox(Modifier.fillMaxWidth().height(48.dp).clip(Theme.shapes.circle))

        Row(
            horizontalArrangement = Arrangement.spacedBy(
                space = Theme.spacing.small,
                alignment = Alignment.CenterHorizontally,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            repeat(6) {
                ShimmerBox(Modifier.size(Theme.size.avatarSmall).clip(CircleShape))
            }
        }

        repeat(5) {
            ShimmerBox(Modifier.fillMaxWidth().height(24.dp).clip(Theme.shapes.small))
        }
    }
}
