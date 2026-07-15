package com.example.designsystem.components.card

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.Theme

/**
 * A nested card meant to sit inside another card's body — a soft grey container
 * ([Theme.colors.disable], ≈ `#FAFAFA`) with rounded corners and no elevation.
 *
 * Pass [onDismiss] when the inner content has an editing/removal action; the helper
 * renders a circular `×` button in the top-end corner that calls it. Leave [onDismiss]
 * null for a pure container.
 */
@Composable
fun InnerContentCard(
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    containerColor: Color = Theme.colors.disable,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
        ) {
            if (onDismiss != null) {
                DismissCircleButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            content()
        }
    }
}

@Composable
private fun DismissCircleButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(width = 1.dp, color = Theme.colors.hint, shape = CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_cancel),
            contentDescription = null,
            tint = Theme.colors.hint,
            modifier = Modifier.size(12.dp),
        )
    }
}
