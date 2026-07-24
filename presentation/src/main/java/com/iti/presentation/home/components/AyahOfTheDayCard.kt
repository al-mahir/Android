package com.iti.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.theme.Theme
import com.iti.domain.model.AyahOfTheDay
import com.iti.presentation.R

private val CardShape = RoundedCornerShape(20.dp)
private val SurahLabelShape = RoundedCornerShape(50)

/**
 * "Ayah of the Day" card.
 *
 * Layout:
 *   ┌─────────────────────────────────────────────────────────┐
 *   │ AYAH OF THE DAY (tertiary/amber)  │ 📖 Maryam • Ayah 58 │
 *   │─────────────────────────────────────────────────────────│
 *   │                  Arabic ayah text                       │
 *   │                  (large, centered)                      │
 *   │─────────────────────────────────────────────────────────│
 *   └─────────────────────────────────────────────────────────┘
 *
 * Card background: [Theme.colors.surfaceContainer].
 * "AYAH OF THE DAY" label color: [Theme.colors.amber] (#7A5800 light).
 * Surah badge: outlined chip on the right.
 */
@Composable
fun AyahOfTheDayCard(
    ayah: AyahOfTheDay,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Theme.colors.surfaceContainer)
            .padding(Theme.spacing.medium),
    ) {
        // ── Top row: label + surah badge ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // "AYAH OF THE DAY"
            BasicText(
                text = stringResource(R.string.home_ayah_of_the_day_label),
                style = Theme.typography.body.small.copy(
                    color = Theme.colors.amber,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )

            Spacer(modifier = Modifier.width(Theme.spacing.small))

            // Surah chip: 📖 SurahName • Ayah N
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(SurahLabelShape)
                    .background(Theme.colors.primaryContainer.copy(alpha = 0.45f))
                    .padding(horizontal = Theme.spacing.small, vertical = 4.dp),
            ) {
                Image(
                    painter = painterResource(DesignSystemR.drawable.ic_book),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Theme.colors.primary),
                    modifier = Modifier.size(12.dp),
                )
                BasicText(
                    text = stringResource(
                        R.string.home_ayah_surah_label,
                        ayah.surahName,
                        ayah.ayahNumber,
                    ),
                    style = Theme.typography.body.small.copy(
                        color = Theme.colors.primary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ── Divider ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Theme.colors.border),
        )

        // ── Arabic ayah text ─────────────────────────────────────────────────
        BasicText(
            text = ayah.arabicText,
            style = Theme.typography.title.copy(
                color = Theme.colors.primaryFont,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
                fontSize = 22.sp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Theme.spacing.small),
        )
    }
}
