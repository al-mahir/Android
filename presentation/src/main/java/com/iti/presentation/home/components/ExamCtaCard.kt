package com.iti.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import com.example.designsystem.theme.Theme
import com.iti.presentation.R

@Composable
fun ExamCtaCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Theme.shapes.large)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Theme.colors.primary,
                        Theme.colors.primary.copy(alpha = 0.7f)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(Theme.spacing.large),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.exam_cta_title),
                style = Theme.typography.title,
                color = Theme.colors.onPrimary
            )
            Text(
                text = stringResource(R.string.exam_cta_subtitle),
                style = Theme.typography.body.medium,
                color = Theme.colors.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = Theme.spacing.extraSmall)
            )
        }
    }
}
