package com.iti.presentation.bookmark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.designsystem.theme.Theme
import com.iti.presentation.R
import com.iti.presentation.bookmark.BookmarkListItem
import com.iti.presentation.sheikh.SheikhInitialsAvatar

@Composable
fun SurahBookmarkCard(
    item: BookmarkListItem.SurahItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BookmarkCardShell(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nameEn,
                style = Theme.typography.body.large,
                color = Theme.colors.primaryFont,
            )
            Spacer(Modifier.height(4.dp))
            val meta = item.startPage?.let { page ->
                stringResource(R.string.bookmark_surah_meta_format, item.ayahCount, page)
            } ?: stringResource(R.string.bookmark_surah_ayah_count_format, item.ayahCount)
            Text(
                text = meta,
                style = Theme.typography.body.small,
                color = Theme.colors.secondaryFont,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.nameAr,
            style = Theme.typography.title,
            color = Theme.colors.primaryFont,
        )
        Spacer(modifier = Modifier.width(8.dp))
        RemoveBookmarkButton(onClick = onRemove)
    }
}

private val AyahCardShape = RoundedCornerShape(20.dp)
private val AyahCardSurahLabelShape = RoundedCornerShape(50)


@Composable
fun AyahBookmarkCard(
    item: BookmarkListItem.AyahItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .clip(AyahCardShape)
            .background(Theme.colors.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(Theme.spacing.medium),
    ) {
        // ── Top row: remove action + surah badge ───────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoveBookmarkButton(onClick = onRemove)

            Spacer(modifier = Modifier.width(Theme.spacing.small))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(AyahCardSurahLabelShape)
                    .background(Theme.colors.primaryContainer.copy(alpha = 0.45f))
                    .padding(horizontal = Theme.spacing.small, vertical = 4.dp),
            ) {
                Icon(
                    painter = painterResource(com.example.designsystem.R.drawable.ic_book),
                    contentDescription = null,
                    tint = Theme.colors.primary,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(
                        R.string.bookmark_ayah_reference_format,
                        item.surahNameAr,
                        item.surahNumber,
                        item.ayahNumber,
                    ),
                    style = Theme.typography.body.small.copy(fontSize = 11.sp),
                    color = Theme.colors.primary,
                )
            }
        }

        // ── Divider ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Theme.colors.border),
        )

        // ── Arabic ayah text ─────────────────────────────────────────────
        Text(
            text = item.ayahText ?: stringResource(R.string.bookmark_ayah_text_unavailable),
            style = Theme.typography.title.copy(
                color = Theme.colors.primaryFont,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
                fontSize = 22.sp,
                textDirection = TextDirection.Rtl,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Theme.spacing.small),
        )

        item.page?.let { page ->
            Text(
                text = stringResource(R.string.bookmark_page_label_format, page),
                style = Theme.typography.body.small,
                color = Theme.colors.secondaryFont,
            )
        }
    }
}

@Composable
fun PageBookmarkCard(
    item: BookmarkListItem.PageItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BookmarkCardShell(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Theme.colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.pageNumber.toString(),
                style = Theme.typography.body.medium,
                color = Theme.colors.primary,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.bookmark_page_label_format, item.pageNumber),
                style = Theme.typography.body.large,
                color = Theme.colors.primaryFont,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.bookmark_page_meta_format, item.surahNameAr, item.juz),
                style = Theme.typography.body.small,
                color = Theme.colors.secondaryFont,
            )
        }
        RemoveBookmarkButton(onClick = onRemove)
    }
}

@Composable
fun SheikhBookmarkCard(
    item: BookmarkListItem.SheikhItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BookmarkCardShell(onClick = onClick, modifier = modifier) {
        SheikhInitialsAvatar(initials = item.initials, sheikhId = item.sheikhId)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = Theme.typography.body.medium,
                color = Theme.colors.primaryFont,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = item.rating.toString(),
                    style = Theme.typography.body.small,
                    color = Theme.colors.secondaryFont,
                )
            }
        }
        RemoveBookmarkButton(onClick = onRemove)
    }
}

@Composable
private fun BookmarkCardShell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Theme.colors.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun RemoveBookmarkButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(com.example.designsystem.R.drawable.ic_bookmark_filled),
            contentDescription = stringResource(R.string.bookmark_cd_remove),
            tint = Theme.colors.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}
