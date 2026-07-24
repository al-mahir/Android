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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.Surah
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.font.PageFontProvider
import com.example.mushaf.presentation.font.rememberSurahNameFontFamily

@Composable
fun SurahListItem(
    surah: Surah,
    query: String = "",
    onClick: (Surah) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(surah) },
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
                    text = surah.number.toString(),
                    style = Theme.typography.body.small,
                    color = Theme.colors.primaryFont
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameEn.highlight(query, Theme.colors.primary),
                    style = Theme.typography.body.large.copy(fontWeight = FontWeight.Bold),
                    color = Theme.colors.primaryFont
                )
                Spacer(Modifier.height(2.dp))
                val revType = if (surah.revelationType.lowercase().startsWith("mec")) {
                    stringResource(R.string.revelation_meccan)
                } else {
                    stringResource(R.string.revelation_medinan)
                }
                Text(
                    text = "$revType · ${stringResource(R.string.verses_count_format, surah.verseCount)}",
                    style = Theme.typography.body.small,
                    color = Theme.colors.hint
                )
            }

            // Arabic Surah Name
            val qpcFont = rememberSurahNameFontFamily()
            val glyph = PageFontProvider.surahNameGlyph(surah.number)
            if (qpcFont != null && glyph != null) {
                Text(
                    text = glyph,
                    fontFamily = qpcFont,
                    style = Theme.typography.title,
                    color = Theme.colors.primaryFont
                )
            } else {
                Text(
                    text = surah.nameAr.highlight(query, Theme.colors.primary),
                    style = Theme.typography.title,
                    color = Theme.colors.primaryFont
                )
            }
        }
    }
}
