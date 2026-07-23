package com.example.mushaf.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.TafsirResult
import androidx.compose.ui.text.style.TextAlign

@Composable
fun TafsirListItem(
    tafsir: TafsirResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = "${tafsir.surahNameArabic} - الآية ${tafsir.ayahNumber}",
            style = Theme.typography.body.medium.copy(
                fontWeight = FontWeight.Bold,
                color = Theme.colors.primary,
                fontSize = 16.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        val parsedHtml = HtmlCompat.fromHtml(tafsir.tafsirText, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
        Text(
            text = parsedHtml,
            style = Theme.typography.body.medium.copy(
                color = Theme.colors.primaryFont,
                lineHeight = 24.sp
            ),
            textAlign = TextAlign.Right
        )
    }
}
