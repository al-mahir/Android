package com.iti.presentation.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.locale.isArabicLocale
import com.example.mushaf.domain.model.SurahCatalog
import com.example.designsystem.components.button.ButtonHeightCompact
import com.example.designsystem.components.button.ButtonIconPosition
import com.example.designsystem.components.button.PrimaryButton
import com.example.designsystem.R as DesignSystemR
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.domain.model.ReadingProgress

private val CardShape = RoundedCornerShape(20.dp)
private val CardImageHeight = 200.dp
private const val SupportingAlpha = 0.65f

@Composable
fun ContinueReadingCard(
    progress: ReadingProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val completionFraction by remember(progress.surahReadAyahs, progress.surahTotalAyahs) {
        derivedStateOf {
            if (progress.surahTotalAyahs > 0) {
                (progress.surahReadAyahs.toFloat() / progress.surahTotalAyahs).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }
    val completionPercent = (completionFraction * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Theme.colors.surfaceContainer)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Image(
            painter = painterResource(DesignSystemR.drawable.card_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(CardImageHeight),
        )

        // ── Info area ──────────────────────────────────────────────────────────
        Column(
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Theme.spacing.medium,
                    vertical = Theme.spacing.medium,
                ),
        ) {
            // Surah name — the domain hands us the surah *number*; the localized name comes from
            // the catalog so the card follows the in-app language instead of always showing English.
            val isArabic = isArabicLocale()
            val surahName = remember(progress.surahNumber, isArabic, progress.surahName) {
                val surah = SurahCatalog.all.firstOrNull { it.number == progress.surahNumber }
                when {
                    surah == null -> progress.surahName
                    isArabic -> surah.nameArabic
                    else -> surah.nameEnglish
                }
            }

            BasicText(
                text = surahName,
                style = Theme.typography.title.copy(
                    color = Theme.colors.primaryFont,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Ayah + Juz
            BasicText(
                text = stringResource(
                    R.string.home_ayah_juz,
                    progress.ayahNumber,
                    progress.juzNumber,
                ),
                style = Theme.typography.body.small.copy(
                    color = Theme.colors.secondaryFont.copy(alpha = SupportingAlpha),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Progress bar
            LinearProgressIndicator(
                progress = { completionFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = Theme.colors.primary,
                trackColor = Theme.colors.border,
                strokeCap = StrokeCap.Round,
            )

            // Percentage + resume button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = stringResource(R.string.home_surah_progress_percent, completionPercent),
                    style = Theme.typography.body.small.copy(
                        color = Theme.colors.secondaryFont,
                        fontWeight = FontWeight.Medium,
                    ),
                )

                PrimaryButton(
                    caption = stringResource(R.string.home_resume_reading),
                    iconPainter = painterResource(DesignSystemR.drawable.ic_arrow_back_rotated),
                    iconPosition = ButtonIconPosition.End,
                    modifier = Modifier.width(ButtonHeightCompact * 3),
                    height = 36.dp,
                    onClick = onClick,
                    captionStyle = Theme.typography.body.small.copy(fontWeight = FontWeight.SemiBold),
                    shape = Theme.shapes.circle
                )
            }
        }
    }
}
