package com.example.mushaf.presentation.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.AyahSearchResult

@Composable
fun AyahListItem(
    ayah: AyahSearchResult,
    onClick: (AyahSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(ayah) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Theme.colors.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, Theme.colors.hint)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = "${ayah.surahNameArabic} (${ayah.surahNameEnglish}) - Ayah ${ayah.ayahNumber}",
                style = Theme.typography.body.small,
                color = Theme.colors.hint
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = ayah.ayahText,
                style = Theme.typography.body.large.copy(fontWeight = FontWeight.Bold),
                color = Theme.colors.primaryFont
            )
        }
    }
}
