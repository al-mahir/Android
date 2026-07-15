package com.example.designsystem.components.bottomnav

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.Theme

internal val BottomNavBarHeight: Dp = 82.dp
private val BottomNavBarShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp,
)
private val BottomNavBarShadowElevation: Dp = 6.dp

@Composable
fun BottomNavBar(
    tabs: List<BottomNavTab>,
    selectedIndex: Int,
    onTabSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = BottomNavBarShadowElevation, shape = BottomNavBarShape)
            .background(Theme.colors.backGround,BottomNavBarShape)
            .clip(BottomNavBarShape)
            .navigationBarsPadding()
    ) {
        tabs.forEachIndexed { index, tab ->
            NavigationTabItem(
                tabData = tab,
                isSelected = index == selectedIndex,
                onClick = { onTabSelected(index) },
            )
        }
    }
}

@Composable
private fun RowScope.NavigationTabItem(
    tabData: BottomNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clip(Theme.shapes.small)
            .clickable(onClick = onClick)
            .padding(top = 16.dp)
            .padding(vertical = 2.dp)
            .semantics {
                role = Role.Tab
                selected = isSelected
            },
    ) {
        Image(
            painter = if (isSelected) tabData.selectedIcon else tabData.icon,
            contentDescription = tabData.title,
        )
        BasicText(
            text = tabData.title,
            style = if (isSelected) {
                Theme.typography.body.small.copy(color = Theme.colors.primary)
            } else {
                Theme.typography.body.small.copy(color = Theme.colors.primaryVariant)
            },
        )
    }
}
