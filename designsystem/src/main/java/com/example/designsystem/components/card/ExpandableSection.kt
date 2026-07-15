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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.Theme

/**
 * A collapsible card: a [containerColor] header carrying the [title] and a circular
 * rotating chevron, over a [bodyColor] body shown only while [expanded]. Expansion state
 * is hoisted — the caller owns [expanded] and toggles it from [onToggle]. Collapsed, the
 * card is just the header.
 */
@Composable
fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Theme.colors.surfaceVariant,
    bodyColor: Color = Theme.colors.backGround,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = Theme.typography.body.large.copy(
                        color = Theme.colors.primaryFont,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                ChevronToggle(expanded = expanded)
            }
            AnimatedVisibility(visible = expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bodyColor),
                ) {
                    content()
                }
            }
        }
    }
}

/** Circular chevron that animates between pointing down (collapsed) and up (expanded). */
@Composable
private fun ChevronToggle(expanded: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron-rotation",
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
