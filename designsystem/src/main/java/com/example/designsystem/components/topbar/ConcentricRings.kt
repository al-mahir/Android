package com.example.designsystem.components.topbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Two faint white concentric rings anchored to the top-trailing corner — the brand
 * backdrop motif. Pass it as the `decoration` of [BackTitleTopBar] (or any branded
 * header). The inner ring is tangent to the top edge (`centerY == innerRadius`), and the
 * anchor flips with layout direction so it always hugs the trailing corner.
 */
@Composable
fun ConcentricRings(modifier: Modifier = Modifier) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier = modifier.fillMaxSize()) {
        val outerRadius = 163.dp.toPx()
        val innerRadius = 75.dp.toPx()
        val anchorInsetX = 52.dp.toPx()
        val centerX = if (isRtl) anchorInsetX else size.width - anchorInsetX
        val center = Offset(centerX, innerRadius)
        drawCircle(color = Color.White.copy(alpha = 0.08f), radius = outerRadius, center = center)
        drawCircle(color = Color.White.copy(alpha = 0.12f), radius = innerRadius, center = center)
    }
}
