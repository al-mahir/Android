package com.example.designsystem.components.bottomnav

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

@Immutable
data class BottomNavTab(
    val title: String,
    val icon: Painter,
    val selectedIcon: Painter,
)
