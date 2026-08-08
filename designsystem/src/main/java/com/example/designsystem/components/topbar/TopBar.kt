package com.example.designsystem.components.topbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.app.Activity
import androidx.core.view.WindowCompat
import com.example.designsystem.R
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

internal val TopBarHeight: Dp = 72.dp
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
    height: Dp = TopBarHeight,
    extendsUnderStatusBar: Boolean = true,
) {
    // Status-bar inset, read even where an ancestor consumed it (Scaffold/statusBarsPadding),
    // so the bar can stretch its background up behind the system status bar.
    val statusBarTop = if (extendsUnderStatusBar) windowStatusBarTop() else 0.dp
    if (extendsUnderStatusBar) {
        // The bar is dark behind the status bar, so the status-bar icons must be light.
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.context as? Activity)?.window
            if (window == null) {
                onDispose { }
            } else {
                val controller = WindowCompat.getInsetsController(window, view)
                val previousLightStatusBars = controller.isAppearanceLightStatusBars
                controller.isAppearanceLightStatusBars = false
                onDispose { controller.isAppearanceLightStatusBars = previousLightStatusBars }
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + statusBarTop)
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
        // Content sits in a full-size container; when extending behind the status bar it is
        // padded by the status bar inset, so title/start/end stay vertically centred in the
        // compact bar below it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (extendsUnderStatusBar) Modifier.statusBarsPadding() else Modifier),
        ) {
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
}

@Composable
private fun windowStatusBarTop(): Dp {
    // WindowInsets.statusBars reflects the real system inset even where an ancestor consumed
    // it (e.g. Scaffold / statusBarsPadding), so the bar can stretch behind the status bar.
    val density = LocalDensity.current
    val top = WindowInsets.statusBars.getTop(density)
    return with(density) { top.toDp() }
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
