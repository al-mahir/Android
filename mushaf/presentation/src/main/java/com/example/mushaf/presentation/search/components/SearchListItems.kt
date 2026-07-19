package com.example.mushaf.presentation.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.Hizb
import com.example.mushaf.domain.model.Juz

@Composable
fun JuzListItem(
    juz: Juz,
    onClick: (Juz) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(juz) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Theme.colors.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, Theme.colors.hint)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Theme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = juz.number.toString(),
                    style = Theme.typography.body.small,
                    color = Theme.colors.primaryFont
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = juz.nameEn,
                    style = Theme.typography.body.large.copy(fontWeight = FontWeight.Bold),
                    color = Theme.colors.primaryFont
                )
            }

            Text(
                text = juz.nameAr,
                style = Theme.typography.title,
                color = Theme.colors.primaryFont
            )
        }
    }
}

@Composable
fun HizbListItem(
    hizb: Hizb,
    onClick: (Hizb) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(hizb) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Theme.colors.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, Theme.colors.hint)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hizb ${hizb.number}",
                style = Theme.typography.body.large.copy(fontWeight = FontWeight.Bold),
                color = Theme.colors.primaryFont,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PageListItem(
    page: Int,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(page) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Theme.colors.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, Theme.colors.hint)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Page $page",
                style = Theme.typography.body.large.copy(fontWeight = FontWeight.Bold),
                color = Theme.colors.primaryFont,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
