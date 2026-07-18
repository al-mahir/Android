package com.iti.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.loading.ShimmerBox
import com.example.designsystem.theme.Theme

private val SkeletonShape = RoundedCornerShape(16.dp)


@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.large),
    ) {
        ShimmerBox(Modifier.width(180.dp).height(28.dp).clip(SkeletonShape))
        ShimmerBox(Modifier.fillMaxWidth().height(48.dp).clip(SkeletonShape))
        ShimmerBox(Modifier.fillMaxWidth().height(104.dp).clip(SkeletonShape))

        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
            repeat(2) {
                ShimmerBox(Modifier.width(148.dp).height(168.dp).clip(SkeletonShape))
            }
        }

        repeat(2) {
            ShimmerBox(Modifier.fillMaxWidth().height(76.dp).clip(SkeletonShape))
        }
    }
}
