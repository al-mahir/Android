package com.example.mushaf.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.R

@Composable
fun MushafTopBar(
    visible: Boolean,
    surahName: String,
    juzNumber: Int,
    hizbNumber: Int,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onBookmark: () -> Unit,
    onSettings: () -> Unit,
    onSurahNameClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.colors.surface.copy(alpha = 0.96f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.mushaf_cd_back),
                    tint = Theme.colors.onSurface,
                    modifier = Modifier.size(Theme.size.iconMedium),
                )
            }

            // Clickable surah name pill
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp), // reduced horizontal space
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // Tappable surah name pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onSurahNameClick)
                        .background(Theme.colors.primary.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = surahName.replace("سورة ", ""),
                        style = Theme.typography.body.large,
                        color = Theme.colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "  |  ",
                    style = Theme.typography.body.medium,
                    color = Theme.colors.secondaryFont,
                )
                Text(
                    text = stringResource(R.string.mushaf_juz_label, juzNumber),
                    style = Theme.typography.body.medium,
                    color = Theme.colors.secondaryFont,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp) // space between icons
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "بحث",
                        tint = Theme.colors.onSurface,
                        modifier = Modifier.size(Theme.size.iconMedium),
                    )
                }
                IconButton(
                    onClick = onBookmark,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = stringResource(R.string.mushaf_cd_bookmark),
                        tint = if (isBookmarked) Theme.colors.primary else Theme.colors.onSurface,
                        modifier = Modifier.size(Theme.size.iconMedium),
                    )
                }
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.mushaf_cd_settings),
                        tint = Theme.colors.onSurface,
                        modifier = Modifier.size(Theme.size.iconMedium),
                    )
                }
            }
        }
    }
}
