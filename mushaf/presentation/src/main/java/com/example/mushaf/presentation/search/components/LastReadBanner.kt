package com.example.mushaf.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.R as DesignSystemR
import com.example.mushaf.presentation.R
import com.example.designsystem.theme.Theme

@Composable
fun LastReadBanner(
    surahNameAr: String,
    surahNameEn: String,
    ayah: Int,
    page: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.colors.primary)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Theme.colors.surface.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = DesignSystemR.drawable.ic_info),
                contentDescription = null,
                tint = Theme.colors.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.last_read),
                color = Theme.colors.onPrimary.copy(alpha = 0.7f),
                style = Theme.typography.body.small.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = stringResource(R.string.last_read_surah_name, surahNameAr, surahNameEn),
                color = Theme.colors.onPrimary,
                style = Theme.typography.body.large.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = stringResource(R.string.ayah_and_page_format, ayah, page),
                color = Theme.colors.onPrimary.copy(alpha = 0.9f),
                style = Theme.typography.body.small
            )
        }
    }
}
