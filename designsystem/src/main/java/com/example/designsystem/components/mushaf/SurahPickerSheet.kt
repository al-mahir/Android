package com.example.designsystem.components.mushaf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.theme.Theme

/** Lightweight model for the Surah Picker — keeps designsystem independent of the domain module. */
data class SurahItem(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val isMeccan: Boolean,
)

/**
 * Full-screen bottom sheet that lists all 114 surahs with a live search bar.
 * Tapping a surah calls [onSurahSelected] and the caller is responsible for dismissing.
 */
@Composable
fun SurahPickerSheet(
    surahs: List<SurahItem>,
    currentSurahNumber: Int,
    onSurahSelected: (SurahItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, surahs) {
        if (query.isBlank()) surahs
        else surahs.filter { s ->
            s.nameArabic.contains(query, ignoreCase = true) ||
                s.nameEnglish.contains(query, ignoreCase = true) ||
                s.number.toString() == query.trim()
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to current surah when the sheet first opens
    LaunchedEffect(currentSurahNumber) {
        val idx = filtered.indexOfFirst { it.number == currentSurahNumber }
        if (idx >= 0) listState.scrollToItem(idx)
    }

    AppBottomSheet(onDismiss = onDismiss, skipPartiallyExpanded = false) {
        // Title
        Text(
            text = "اختر السورة",
            style = Theme.typography.title.copy(fontWeight = FontWeight.Bold),
            color = Theme.colors.primaryFont,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Center,
        )

        // Search field
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
                Text(
                    "ابحث بالاسم أو الرقم…",
                    style = Theme.typography.body.medium,
                    color = Theme.colors.hint,
                )
            },
            leadingIcon = {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(com.example.designsystem.R.drawable.ic_search),
                    contentDescription = null,
                    tint = Theme.colors.hint,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Theme.colors.primary,
                unfocusedBorderColor = Theme.colors.border,
                focusedContainerColor = Theme.colors.surface,
                unfocusedContainerColor = Theme.colors.surface,
                cursorColor = Theme.colors.primary,
                focusedTextColor = Theme.colors.primaryFont,
                unfocusedTextColor = Theme.colors.primaryFont,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )

        HorizontalDivider(color = Theme.colors.border)

        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            items(filtered, key = { it.number }) { surah ->
                SurahRow(
                    surah = surah,
                    isSelected = surah.number == currentSurahNumber,
                    onClick = { onSurahSelected(surah) },
                )
                HorizontalDivider(
                    color = Theme.colors.border.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SurahRow(
    surah: SurahItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Theme.colors.primary.copy(alpha = 0.08f)
                else Theme.colors.backGround
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        // Surah number badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) Theme.colors.primary else Theme.colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = surah.number.toString(),
                style = Theme.typography.body.small.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) Theme.colors.onPrimary else Theme.colors.secondaryFont,
            )
        }

        Spacer(Modifier.width(12.dp))

        // Names (RTL: Arabic on top, English below)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = surah.nameArabic,
                style = Theme.typography.body.large.copy(fontWeight = FontWeight.Medium),
                color = if (isSelected) Theme.colors.primary else Theme.colors.primaryFont,
                textAlign = TextAlign.End,
            )
            Text(
                text = surah.nameEnglish,
                style = Theme.typography.body.small,
                color = Theme.colors.secondaryFont,
                textAlign = TextAlign.End,
            )
        }

        Spacer(Modifier.width(10.dp))

        // Meccan / Medinan badge
        val originLabel = if (surah.isMeccan) "مكية" else "مدنية"
        val originColor = if (surah.isMeccan)
            Theme.colors.amber.copy(alpha = 0.15f) else Theme.colors.primary.copy(alpha = 0.12f)
        val originTextColor = if (surah.isMeccan)
            Theme.colors.amber else Theme.colors.primary

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(originColor)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = originLabel,
                style = Theme.typography.body.small.copy(fontWeight = FontWeight.Medium),
                color = originTextColor,
            )
        }
    }
}
