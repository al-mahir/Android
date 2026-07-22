package com.iti.presentation.staticcontent.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.designsystem.components.bullet.AccentBullet
import com.example.designsystem.theme.Theme

@Composable
internal fun MarkdownBlockText(
    block: MarkdownBlock,
    modifier: Modifier = Modifier,
) {
    when (block) {
        is MarkdownBlock.Heading -> BasicText(
            text = block.text,
            style = if (block.level == 1) {
                Theme.typography.title.copy(color = Theme.colors.primaryFont)
            } else {
                Theme.typography.body.large.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            modifier = modifier,
        )

        is MarkdownBlock.Bullet -> Row(
            modifier = modifier.padding(start = Theme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            AccentBullet(color = Theme.colors.primary)
            BasicText(
                text = block.text,
                style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
            )
        }

        is MarkdownBlock.Paragraph -> BasicText(
            text = block.text,
            style = Theme.typography.body.medium.copy(color = Theme.colors.secondaryFont),
            modifier = modifier,
        )
    }
}
