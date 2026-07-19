package com.example.mushaf.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.designsystem.theme.Theme
import com.example.mushaf.presentation.R

@Composable
fun PageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.mushaf_page_indicator, currentPage, pageCount),
        style = Theme.typography.body.medium,
        color = Theme.colors.onSurface,
        modifier = modifier,
    )
}
