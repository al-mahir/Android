package com.example.mushaf.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.designsystem.theme.Theme

// Helpers

private fun Int.toArabicNumerals(): String {
    val eastern = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return toString().map { eastern[it - '0'] }.joinToString("")
}

@Composable
fun MushafPageOverlay(
    pageNumber: Int,
    juzNumber: Int,
    hizbQuarterInHizb: Int,
    isRightPage: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JuzHizbChip(juzNumber = juzNumber, quarterInHizb = hizbQuarterInHizb)
            PageNumberChip(pageNumber = pageNumber, isRightPage = isRightPage)
        }

        PageFaceEdgeCue(
            isRightPage = isRightPage,
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                // Inside pager, layout direction is RTL
                // Right page (odd) -> Right edge -> Start
                // Left page (even) -> Left edge -> End
                .align(if (isRightPage) Alignment.CenterStart else Alignment.CenterEnd),
        )
    }
}

@Composable
private fun JuzHizbChip(juzNumber: Int, quarterInHizb: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Theme.colors.surface.copy(alpha = 0.92f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HizbQuarterDiagram(quarterInHizb = quarterInHizb)
        Text(
            text = "الجزء ${juzNumber.toArabicNumerals()}",
            style = Theme.typography.body.small.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            ),
            color = Theme.colors.primaryFont,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HizbQuarterDiagram(quarterInHizb: Int) {
    val filledColor = Theme.colors.primary
    val emptyColor = Theme.colors.onSurface.copy(alpha = 0.20f)

    Canvas(modifier = Modifier.size(18.dp)) {
        val strokeWidth = 2.2.dp.toPx()
        val radius = (size.minDimension / 2f) - strokeWidth
        val arcTopLeft = androidx.compose.ui.geometry.Offset(
            x = center.x - radius,
            y = center.y - radius,
        )
        val arcSize = Size(radius * 2, radius * 2)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        for (i in 0 until 4) {
            drawArc(
                color = if (i < quarterInHizb) filledColor else emptyColor,
                startAngle = -90f + i * 90f,
                sweepAngle = 84f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = stroke,
            )
        }
    }
}

@Composable
private fun PageNumberChip(pageNumber: Int, isRightPage: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Theme.colors.surface.copy(alpha = 0.92f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (isRightPage) "يمين" else "يسار",
            style = Theme.typography.body.small.copy(fontSize = 9.sp),
            color = Theme.colors.secondaryFont,
        )
        Text(text = "·", color = Theme.colors.secondaryFont, fontSize = 11.sp)
        Text(
            text = pageNumber.toArabicNumerals(),
            style = Theme.typography.body.small.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            ),
            color = Theme.colors.primaryFont,
        )
    }
}

@Composable
private fun PageFaceEdgeCue(
    isRightPage: Boolean,
    modifier: Modifier = Modifier,
) {
    val primary = Theme.colors.primary
    Canvas(modifier = modifier) {
        val radiusX = 8.dp.toPx()
        val radiusY = 28.dp.toPx()
        val centerY = size.height / 2f
        val path = Path().apply {
            if (isRightPage) {
                // Right page (odd): cue on physical right edge.
                // Bulge points inwards (left). Center is at the right edge (size.width).
                arcTo(
                    rect = Rect(
                        left = size.width - radiusX,
                        top = centerY - radiusY,
                        right = size.width + radiusX,
                        bottom = centerY + radiusY
                    ),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true,
                )
            } else {
                // Left page (even): cue on physical left edge.
                // Bulge points inwards (right). Center is at the left edge (0).
                arcTo(
                    rect = Rect(
                        left = -radiusX,
                        top = centerY - radiusY,
                        right = radiusX,
                        bottom = centerY + radiusY
                    ),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true,
                )
            }
        }
        drawPath(path = path, color = primary.copy(alpha = 0.40f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}
