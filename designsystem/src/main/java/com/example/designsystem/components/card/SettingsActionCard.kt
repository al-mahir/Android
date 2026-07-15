package com.example.designsystem.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme

/**
 * Per-row accent palette for [SettingsActionCard]. Lives in the design system so
 * feature code never hard-codes hex (AGENTS rule). Matches the profile mockups.
 */
object SettingsCardAccent {
    val Purple = Color(0xFF754EB9)
    val Orange = Color(0xFFF5921E)
    val Blue = Color(0xFF2F80ED)
    val Teal = Color(0xFF4D8998)
    val Green = Color(0xFF27AE60)
    val Amber = Color(0xFFF2C94C)
    val Red = Color(0xFFEB5757)
}

/** Leading control rendered on the far (logical) end of a [SettingsActionCard]. */
sealed interface SettingsActionTrailing {
    /** Chevron in a thin circle — the row opens another screen. */
    data object Arrow : SettingsActionTrailing

    /** A switch — the row toggles a setting in place (e.g. biometrics). */
    data class Toggle(
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
    ) : SettingsActionTrailing

    /** No control — the whole card is a single action button (e.g. logout). */
    data object None : SettingsActionTrailing
}

/**
 * Reusable settings row: a soft-shadowed pill with a tinted, accent-arced icon box on
 * the leading edge, a title, and a trailing control. Layout mirrors automatically under
 * RTL. Pure design-system: token-driven, no feature knowledge — drop it anywhere.
 *
 * @param accent per-row colour driving both the icon tint and the edge arc.
 * @param onClick invoked when the whole card is tapped (skip for [SettingsActionTrailing.Toggle]).
 */
@Composable
fun SettingsActionCard(
    title: String,
    icon: Painter,
    accent: Color,
    modifier: Modifier = Modifier,
    chevronIcon: Painter? = null,
    titleColor: Color = Theme.colors.primaryFont,
    trailing: SettingsActionTrailing = SettingsActionTrailing.Arrow,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = Theme.colors.backGround,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Accent crescent: drawn first so it sits on the card background, behind
            // the row content. The Surface clips it to the 18.dp corner radius.
            EdgeCrescent(
                color = accent,
                modifier = Modifier.matchParentSize(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                    .padding(
                        horizontal = Theme.spacing.medium,
                        vertical = Theme.spacing.small,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                AccentIconBox(icon = icon, accent = accent)

                Text(
                    text = title,
                    style = Theme.typography.body.large.copy(fontWeight = FontWeight.Medium),
                    color = titleColor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Theme.spacing.small),
                )

                when (trailing) {
                    SettingsActionTrailing.Arrow -> chevronIcon?.let { ChevronCircle(it) }
                    SettingsActionTrailing.None -> Unit
                    is SettingsActionTrailing.Toggle -> Switch(
                        checked = trailing.checked,
                        onCheckedChange = trailing.onCheckedChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Theme.colors.backGround,
                            checkedTrackColor = Theme.colors.success,
                            uncheckedThumbColor = Theme.colors.backGround,
                            uncheckedTrackColor = Theme.colors.hint,
                            uncheckedBorderColor = Theme.colors.hint,
                        ),
                        // Material 3 Switch ignores height/width — use scale instead.
                        // Default intrinsic size is 52×32 dp; target is 36×20 dp.
                        modifier = Modifier.scale(scaleX = 36f / 52f, scaleY = 20f / 32f),
                    )
                }
            }
        }
    }
}

/**
 * Thin accent stroke that traces the card's **leading rounded-rect edge** — a
 * constant-width `(`-shaped line that curves around the top and bottom corners
 * and runs straight down the start border between them. It is a *stroke*, not a
 * filled crescent: uniform thickness end-to-end, matching the mockups (a faint
 * tinted bracket hugging the corner), not a fat lens that bulges at the centre.
 *
 * Geometry is built once for the LTR (left) edge along the **stroke centreline**,
 * inset by half the stroke width so the line sits fully inside the [Surface]'s
 * rounded clip. The centreline's corner radius is the card radius minus that
 * inset, keeping it concentric with the card border. For RTL the whole path is
 * mirrored about the canvas centre, moving it to the right edge unchanged.
 *
 * @param cornerRadius the host shape's corner radius — pass the same value as the
 *   enclosing [Surface]/[RoundedCornerShape] so the crescent's corners stay
 *   concentric with the clip. Defaults to the [SettingsActionCard] radius.
 * @param bulgeThickness the maximum thickness of the crescent bulge at its vertical
 *   centre. Defaults to the [SettingsActionCard] thickness; pass a smaller value for
 *   slimmer accents on compact hosts.
 */
private const val CARD_CORNER_DP = 16

/** The maximum thickness of the crescent bulge at the exact vertical center. */
private const val BULGE_THICKNESS_DP = 3

@Composable
fun EdgeCrescent(
    color: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CARD_CORNER_DP.dp,
    bulgeThickness: Dp = BULGE_THICKNESS_DP.dp,
) {
    Spacer(modifier = modifier.drawWithCache {
        val cornerPx = cornerRadius.toPx().coerceAtMost(size.height / 2f)
        // Coerce the bulge so it never exceeds the corner radius, which would invert the math
        val bulgePx = bulgeThickness.toPx().coerceAtMost(cornerPx * 0.9f)
        val isRtl = layoutDirection == LayoutDirection.Rtl

        val path = Path().apply {
            arcTo(
                rect = Rect(0f, 0f, 2f * cornerPx, 2f * cornerPx),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -90f,
                forceMoveTo = true,
            )
            lineTo(0f, size.height - cornerPx)

            arcTo(
                rect = Rect(0f, size.height - 2f * cornerPx, 2f * cornerPx, size.height),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false,
            )
            val cx = cornerPx
            val rx = cornerPx - bulgePx
            arcTo(
                rect = Rect(
                    left = cx - rx,
                    top = 0f,
                    right = cx + rx,
                    bottom = size.height
                ),
                startAngleDegrees = 90f,  // Start at 6 o'clock (bottom tip)
                sweepAngleDegrees = 180f, // Sweep clockwise up the left side
                forceMoveTo = false
            )
            close()
        }

        onDrawBehind {
            if (isRtl) {
                // Mirrors perfectly to the far-right edge for Arabic layouts
                scale(scaleX = -1f, scaleY = 1f) {
                    drawPath(path = path, color = color)
                }
            } else {
                drawPath(path = path, color = color)
            }
        }
    })
}
@Composable
private fun AccentIconBox(icon: Painter, accent: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(Theme.size.iconSemiMedium),
        )
    }
}

@Composable
private fun ChevronCircle(chevron: Painter) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .border(width = 1.dp, color = Theme.colors.hint, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = chevron,
            contentDescription = null,
            tint = Theme.colors.hint,
            modifier = Modifier.size(14.dp),
        )
    }
}
@Composable
private fun PreviewCard() {
    SettingsActionCard(
        title = stringResource(R.string.overlay_status_success),
        icon = painterResource(R.drawable.ic_profile),
        accent = SettingsCardAccent.Blue,
        trailing = SettingsActionTrailing.Toggle(checked = true, onCheckedChange = {}),
        onClick = null,
    )
}

@Preview(name = "LTR", locale = "en")
@Composable
private fun PreviewActionCardLtr() {
    AlMahirTheme(isDarkTheme = false, locale = java.util.Locale("en")) {
        PreviewCard()
    }
}

@Preview(name = "RTL", locale = "ar")
@Composable
private fun PreviewActionCardRtl() {
    AlMahirTheme(isDarkTheme = false, locale = java.util.Locale("ar")) {
        PreviewCard()
    }
}
