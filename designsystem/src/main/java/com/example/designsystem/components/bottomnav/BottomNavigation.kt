package com.example.designsystem.components.bottomnav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme

private val BottomNavBarShape = RoundedCornerShape(100.dp)
private val BottomNavBarShadowElevation: Dp = 18.dp
private const val SelectionAnimationMillis = 250


private val TabItemHeight: Dp = 56.dp


@Composable
fun bottomNavBarContentHeight(): Dp =
    TabItemHeight +
        (Theme.spacing.small * 2) +
        (Theme.spacing.small * 2)


@Composable
fun bottomNavBarHeight(): Dp =
    bottomNavBarContentHeight() +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()


@Composable
fun BottomNavBar(
    tabs: List<BottomNavTab>,
    selectedIndex: Int,
    onTabSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = Theme.spacing.medium,
                vertical = Theme.spacing.small,
            ),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(BottomNavBarShadowElevation, BottomNavBarShape, clip = false)
                .clip(BottomNavBarShape)
                .background(Theme.colors.surface)
                .border(1.dp, Theme.colors.border, BottomNavBarShape)
                .padding(Theme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                NavigationTabItem(
                    tabData = tab,
                    isSelected = index == selectedIndex,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavigationTabItem(
    tabData: BottomNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animationSpec = tween<Color>(durationMillis = SelectionAnimationMillis)

    val pillColor by animateColorAsState(
        targetValue = if (isSelected) Theme.colors.primaryContainer else Color.Transparent,
        animationSpec = animationSpec,
        label = "navPill",
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) Theme.colors.onPrimaryContainer else Theme.colors.outline,
        animationSpec = animationSpec,
        label = "navLabel",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(durationMillis = SelectionAnimationMillis),
        label = "navScale",
    )

    Column(
        modifier = modifier
            .height(TabItemHeight)
            .clip(BottomNavBarShape)
            .background(pillColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = Theme.spacing.small)
            .semantics {
                role = Role.Tab
                selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = if (isSelected) tabData.selectedIcon else tabData.icon,
            contentDescription = tabData.title,
            modifier = Modifier
                .size(Theme.size.iconMedium)
                .scale(iconScale),
        )
        BasicText(
            text = tabData.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = Theme.typography.body.small.copy(
                color = labelColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}
