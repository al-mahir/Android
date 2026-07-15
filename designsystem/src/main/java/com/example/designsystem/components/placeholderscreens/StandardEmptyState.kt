package com.example.designsystem.components.placeholderscreens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.theme.Theme

@Composable
fun StandardEmptyState(
    title: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    EmptyState(
        modifier = modifier,
        iconSlot = {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(160.dp)
            )
        },
        titleSlot = {
            BasicText(
                text = title,
                style = Theme.typography.title.copy(
                    color = Theme.colors.primaryFont,
                    textAlign = TextAlign.Center
                ),
            )
        },
        descriptionSlot = description?.let { desc ->
            {
                BasicText(
                    text = desc,
                    style = Theme.typography.body.medium.copy(
                        color = Theme.colors.secondaryFont,
                        textAlign = TextAlign.Center
                    ),
                )
            }
        },
        actionSlot = if (actionButtonText != null && onActionClick != null) {
            {
                PrimaryButton(
                    caption = actionButtonText,
                    onClick = onActionClick
                )
            }
        } else null
    )
}
@Composable
internal fun EmptyState(
    iconSlot: @Composable () -> Unit,
    titleSlot: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    descriptionSlot: (@Composable () -> Unit)? = null,
    actionSlot: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        iconSlot()

        Spacer(modifier = Modifier.height(24.dp))
        titleSlot()

        if (descriptionSlot != null) {
            Spacer(modifier = Modifier.height(8.dp))
            descriptionSlot()
        }

        if (actionSlot != null) {
            Spacer(modifier = Modifier.height(32.dp))
            actionSlot()
        }
    }
}
