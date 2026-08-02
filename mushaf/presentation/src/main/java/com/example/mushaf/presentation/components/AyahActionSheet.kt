package com.example.mushaf.presentation.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.designsystem.components.bottomsheet.AppBottomSheet
import com.example.designsystem.theme.Theme
import com.example.mushaf.domain.model.SurahCatalog
import com.example.mushaf.presentation.R
import com.example.mushaf.presentation.state.AyahActionSheetState

@Composable
fun AyahActionSheet(
    sheetState: AyahActionSheetState,
    onDismiss: () -> Unit,
    onOpenTafsir: () -> Unit,
    onCopy: () -> Unit,
    onOpenNotes: () -> Unit,
) {
    AppBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.ayah_actions_title),
                style = Theme.typography.title.copy(
                    fontWeight = FontWeight.Bold,
                    color = Theme.colors.primaryFont,
                ),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = ayahReferenceLabel(sheetState.surahNumber, sheetState.ayahNumber),
                style = Theme.typography.body.medium,
                color = Theme.colors.secondaryFont,
            )

            Spacer(Modifier.height(12.dp))

            if (sheetState.ayahText.isNotBlank()) {
                Text(
                    text = sheetState.ayahText,
                    style = Theme.typography.body.large.copy(
                        lineHeight = 26.sp,
                        color = Theme.colors.primaryFont,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AyahActionItem(
                    icon = { Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Theme.colors.primary, modifier = Modifier.size(24.dp)) },
                    label = stringResource(R.string.ayah_action_tafsir),
                    onClick = onOpenTafsir,
                )
                AyahActionItem(
                    icon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Theme.colors.primary, modifier = Modifier.size(24.dp)) },
                    label = stringResource(R.string.ayah_action_copy),
                    onClick = onCopy,
                )
                AyahActionItem(
                    icon = { Icon(Icons.Filled.EditNote, contentDescription = null, tint = Theme.colors.primary, modifier = Modifier.size(24.dp)) },
                    label = stringResource(R.string.ayah_action_notes),
                    onClick = onOpenNotes,
                )
            }

            val noteText = sheetState.note?.text?.trim()
            if (!noteText.isNullOrEmpty()) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Theme.colors.surfaceVariant)
                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.ayah_note_saved_title),
                    style = Theme.typography.body.medium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Theme.colors.primary,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = noteText,
                    style = Theme.typography.body.medium,
                    color = Theme.colors.secondaryFont,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.ayah_note_view_edit),
                    style = Theme.typography.body.medium.copy(color = Theme.colors.primary),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenNotes)
                        .padding(4.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AyahActionItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Theme.colors.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = label,
            style = Theme.typography.body.medium.copy(
                fontWeight = FontWeight.Medium,
                color = Theme.colors.onSurface,
            ),
        )
    }
}

@Composable
private fun ayahReferenceLabel(surahNumber: Int, ayahNumber: Int): String {
    val isArabic = LocalConfiguration.current.locales[0].language == "ar"
    val surah = SurahCatalog.all.getOrNull(surahNumber - 1)
    val surahName = if (isArabic) {
        surah?.nameAr ?: "سورة $surahNumber"
    } else {
        surah?.nameEn ?: "Surah $surahNumber"
    }
    return stringResource(R.string.ayah_reference_format, surahName, ayahNumber)
}
