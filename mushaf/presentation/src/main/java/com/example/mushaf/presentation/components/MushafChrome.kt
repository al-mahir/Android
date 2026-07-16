package com.example.mushaf.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mushaf.presentation.R


@Composable
fun MushafChrome(
    currentPage: Int,
    pageCount: Int,
    isTajweedEnabled: Boolean,
    isFollowAlongActive: Boolean,
    onToggleTajweed: (Boolean) -> Unit,
    onToggleFollowAlong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PageIndicator(currentPage = currentPage, pageCount = pageCount)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.mushaf_tajweed_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 4.dp),
            )
            Switch(
                checked = isTajweedEnabled,
                onCheckedChange = onToggleTajweed,
            )
            TextButton(onClick = onToggleFollowAlong) {
                Text(
                    text = stringResource(
                        if (isFollowAlongActive) R.string.mushaf_follow_along_stop
                        else R.string.mushaf_follow_along_start,
                    ),
                )
            }
        }
    }
}

@Composable
fun PageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.mushaf_page_indicator, currentPage, pageCount),
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier,
    )
}
