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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.R
import com.example.mushaf.domain.model.AyahSearchResult

@Composable
fun AyahListItem(
    ayah: AyahSearchResult,
    query: String = "",
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
                text = stringResource(
                    R.string.ayah_title_format,
                    ayah.surahNameArabic,
                    ayah.surahNameEnglish,
                    ayah.ayahNumber
                ),
                style = Theme.typography.body.small,
                color = Theme.colors.hint
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = ayah.ayahText.highlight(query, Theme.colors.primary),
                style = Theme.typography.body.large.copy(
                    fontWeight = FontWeight.Bold,
                    textDirection = TextDirection.Rtl
                ),
                color = Theme.colors.primaryFont,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
            val translation = ayah.translation
            if (!translation.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = translation.highlight(query, Theme.colors.primary),
                    style = Theme.typography.body.medium,
                    color = Theme.colors.secondaryFont,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
