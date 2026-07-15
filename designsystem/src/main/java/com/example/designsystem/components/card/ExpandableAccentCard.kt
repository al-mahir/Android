package com.example.designsystem.components.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.Theme

/**
 * A collapsible card with an [EdgeCrescent] accent on the layout start edge — the
 * meal-plan / nutritional-info row pattern. Caller-supplied [header] (so the row can
 * carry an icon + title + subtitle + meta) and [body], shown only while [expanded].
 *
 * The chevron lives at the layout end of the header row; tapping the header toggles.
 *
 * The card shape is locked to a 16dp corner radius because [EdgeCrescent] traces that
 * same radius — using a different shape would break the crescent's geometry.
 */
@Composable
fun ExpandableAccentCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = Theme.colors.backGround,
    header: @Composable RowScope.() -> Unit,
    body: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Drawn first so the row content sits on top; the Card's shape clips both.
            EdgeCrescent(
                color = accentColor,
                modifier = Modifier.matchParentSize(),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            role = Role.Button,
                            onClick = onToggle,
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    header()
                    AccentChevronToggle(expanded = expanded)
                }
                AnimatedVisibility(visible = expanded) {
                    Box(modifier = Modifier.fillMaxWidth()) { body() }
                }
            }
        }
    }
}

@Composable
private fun AccentChevronToggle(expanded: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "accent-chevron-rotation",
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(width = 1.dp, color = Theme.colors.hint, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = null,
            tint = Theme.colors.hint,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation),
        )
    }
}
