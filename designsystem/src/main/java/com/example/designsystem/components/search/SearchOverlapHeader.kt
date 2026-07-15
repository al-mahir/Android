package com.example.designsystem.components.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.components.topbar.BackTitleTopBar
import com.example.designsystem.components.topbar.TopBarHeight
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

/** How far the search field's bottom edge extends past the top bar. */
private val SearchOverlap = 26.dp

/**
 * A top bar with a [SearchBar] floated across its bottom edge — the field half-overlaps
 * the bar and half-hangs below it. The [topBar] is a slot so each screen can supply its
 * own variant (back-title, plain title, …); this wrapper owns the overlap layout so no
 * screen has to re-derive the bar height.
 */
@Composable
fun SearchOverlapHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TopBarHeight + SearchOverlap),
    ) {
        topBar()
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            hint = hint,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = Theme.spacing.large),
        )
    }
}

@Preview(name = "SearchOverlapHeader – RTL", showBackground = true)
@Composable
private fun SearchOverlapHeaderPreview() {
    AlMahirTheme(locale = Locale("ar")) {
        SearchOverlapHeader(
            query = "",
            onQueryChange = {},
            hint = "بحث برقم الاستشارة او اسم المستشار",
            topBar = { BackTitleTopBar(title = "حجوزاتي", onBackClick = {}) },
        )
    }
}
