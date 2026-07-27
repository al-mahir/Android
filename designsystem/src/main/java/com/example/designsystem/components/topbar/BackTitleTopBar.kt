package com.example.designsystem.components.topbar

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.designsystem.R
import com.example.designsystem.theme.Theme


@Composable
fun BackTitleTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    backIcon: Painter = painterResource(R.drawable.ic_arrow_back),
    iconStyle: TopBarIconStyle = TopBarIconStyle.CircularBackground(),
    decoration: (@Composable BoxScope.() -> Unit)? = null,
    end: (@Composable RowScope.() -> Unit)? = null,
) {
    BaseTopBar(
        modifier = modifier,
        decoration = decoration,
        start = {
            TopBarIcon(
                icon = backIcon,
                contentDescription = stringResource(R.string.topbar_back_content_description),
                onClick = onBackClick,
                style = iconStyle,
            )
        },
        center = {
            BasicText(
                text = title,
                style = Theme.typography.title.copy(
                    color = Theme.colors.onPrimary,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
            )
        },
        end = end ?: {},
    )
}
