package com.example.designsystem.components.mushaf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.color.TajweedColors
import com.example.designsystem.color.TajweedRule
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.theme.Theme

/**
 * Bottom sheet showing a color legend for Tajweed rules using QUL color coding.
 * Does not interact with the Mushaf page — purely informational.
 */
@Composable
fun TajweedLegendSheet(onDismiss: () -> Unit) {
    AppBottomSheet(onDismiss = onDismiss) {
        // Title
        Text(
            text = "دليل ألوان التجويد",
            style = Theme.typography.title.copy(fontWeight = FontWeight.Bold),
            color = Theme.colors.primaryFont,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "نظام ألوان QUL للتجويد",
            style = Theme.typography.body.small,
            color = Theme.colors.hint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Center,
        )

        HorizontalDivider(color = Theme.colors.border)
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(TajweedColors.rules) { rule ->
                TajweedRuleRow(rule)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TajweedRuleRow(rule: TajweedRule) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        // Arabic rule name
        Text(
            text = rule.nameArabic,
            style = Theme.typography.body.medium.copy(fontWeight = FontWeight.Medium),
            color = Theme.colors.primaryFont,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.width(12.dp))

        // Color swatch
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(rule.color)
        )
    }
}
