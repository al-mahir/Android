package com.example.designsystem.components.topbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

internal val TopBarHeight: Dp = 148.dp
internal val TopBarBackgroundImageWidth: Dp = 325.dp
internal val TopBarBackgroundImageOffset: Dp = (200).dp
private val TopBarShape = RoundedCornerShape(0.dp, 0.dp, 40.dp, 40.dp)
private val TopHomeBarShape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)

/**
 * Space reserved on each side of the centred title so a long title wraps instead of
 * sliding under the start/end icons. Symmetric, so the title stays optically centred.
 */
private val TopBarCenterSideReserve: Dp = 64.dp

@Composable
internal fun BaseTopBar(
    modifier: Modifier = Modifier,
    backgroundColor: Brush = Brush.verticalGradient(
        colors = listOf(Theme.colors.primary, Theme.colors.primary)//TODO for home use background
    ),
    shape: Shape = TopBarShape,
    decoration: (@Composable BoxScope.() -> Unit)? = null,
    start: @Composable RowScope.() -> Unit = {},
    center: @Composable RowScope.() -> Unit = {},
    end: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TopBarHeight)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.TopStart,
    ) {
        decoration?.let {
            decoration()
        } ?: Image(
            painter = painterResource(R.drawable.bg),
            contentDescription = null,
            modifier = Modifier
                .size(TopBarBackgroundImageWidth)
                .offset(x = TopBarBackgroundImageOffset),
            contentScale = ContentScale.FillWidth,
            alpha = 0.3f,
        )
        // Center is aligned to the bar's true centre — independent of how wide the
        // start/end slots are — so the title sits in the sharp centre. Start/end hug
        // their edges; the symmetric side reserve keeps a long title off the icons.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = TopBarCenterSideReserve),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = center,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = Theme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            content = start,
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(horizontal = Theme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            content = end,
        )
    }
}

//preview
@Preview
@Composable
fun PreviewTopAppBar() {
    AlMahirTheme(
        locale = Locale("ar")
    ) {
        BaseTopBar()
    }
}