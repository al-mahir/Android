package com.example.designsystem.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme

/** Faint-watermark treatment for [SectionedCard]'s body band. */
private val SectionedCardWatermarkSize = 100.dp
private const val SectionedCardWatermarkAlpha = 0.06f

/**
 * A card split into vertically-stacked sections. The optional [header] (and the [footer]
 * in the overload below) sit on the card's [containerColor]; the [body] is a band painted
 * with its own [bodyColor]. Each section is a caller-supplied composable that brings its
 * own padding and layout — [SectionedCard] only owns the card shape and the section bands.
 *
 * [bodyWatermark], when supplied, is painted faintly behind the body content, anchored to
 * the band's bottom-end corner and cropped to the band — it does not affect the band's size.
 *
 * This base overload renders header + body; the overload below adds a footer section.
 */
@Composable
fun SectionedCard(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    containerColor: Color = Theme.colors.surfaceVariant,
    bodyColor: Color = Theme.colors.backGround,
    bodyWatermark: Painter? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    body: @Composable () -> Unit,
) {
    SectionedCardContainer(
        modifier = modifier,
        header = header,
        footer = null,
        containerColor = containerColor,
        bodyColor = bodyColor,
        bodyWatermark = bodyWatermark,
        shape = shape,
        body = body,
    )
}

/**
 * [SectionedCard] with an added [footer] section, painted on [containerColor] below the
 * body band — for cards that carry a trailing action (e.g. "view details", "rate").
 */
@Composable
fun SectionedCard(
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    containerColor: Color = Theme.colors.surfaceVariant,
    bodyColor: Color = Theme.colors.backGround,
    bodyWatermark: Painter? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    body: @Composable () -> Unit,
) {
    SectionedCardContainer(
        modifier = modifier,
        header = header,
        footer = footer,
        containerColor = containerColor,
        bodyColor = bodyColor,
        bodyWatermark = bodyWatermark,
        shape = shape,
        body = body,
    )
}

@Composable
private fun SectionedCardContainer(
    modifier: Modifier,
    header: (@Composable () -> Unit)?,
    footer: (@Composable () -> Unit)?,
    containerColor: Color,
    bodyColor: Color,
    bodyWatermark: Painter?,
    shape: Shape,
    body: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            header?.let { Box(modifier = Modifier.fillMaxWidth()) { it() } }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bodyColor)
                    .clipToBounds()
                    .drawWatermark(bodyWatermark),
            ) {
                body()
            }
            footer?.let { Box(modifier = Modifier.fillMaxWidth()) { it() } }
        }
    }
}

/**
 * Paints [watermark] faintly at the body band's bottom-end corner. Drawn (not laid out) so
 * it never grows the band — the part taller/wider than the band is cropped by `clipToBounds`.
 */
private fun Modifier.drawWatermark(watermark: Painter?): Modifier {
    if (watermark == null) return this
    return drawBehind {
        val targetWidth = SectionedCardWatermarkSize.toPx()
        val intrinsic = watermark.intrinsicSize
        val aspect = if (intrinsic.isSpecified && intrinsic.width > 0f) {
            intrinsic.height / intrinsic.width
        } else {
            1f
        }
        val targetHeight = targetWidth * aspect
        val isRtl = layoutDirection == LayoutDirection.Rtl
        // Top flush (bottom crops off); start-side, bleeding ~10% past the edge.
        val left = if (isRtl) size.width - targetWidth * 0.90f else -targetWidth * 0.10f
        val top = 0f
        translate(left = left, top = top) {
            with(watermark) {
                draw(size = Size(targetWidth, targetHeight), alpha = SectionedCardWatermarkAlpha)
            }
        }
    }
}
