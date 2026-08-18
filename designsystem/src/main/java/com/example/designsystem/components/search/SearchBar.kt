package com.example.designsystem.components.search

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.components.textfield.TextField
import com.example.designsystem.theme.AlMahirTheme
import java.util.Locale

/**
 * Reusable single-line search field — a thin wrapper over [TextField] with a 14.dp
 * corner radius and a leading search icon. The [hint] is caller-supplied so each host
 * screen can localise it.
 *
 * @param onClear when supplied, a clear affordance appears at the end of the field as soon as
 *   [query] is non-empty, so wiping a search never costs a long press on backspace. Screens that
 *   have no meaningful "empty search" state leave it null and the field stays plain.
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    val showClear = onClear != null && query.isNotEmpty()
    TextField(
        text = query,
        onTextChange = onQueryChange,
        modifier = modifier,
        hint = hint,
        leadingIcon = painterResource(R.drawable.ic_search),
        trailingIcon = if (showClear) painterResource(R.drawable.ic_cancel) else null,
        trailingIconContentDescription = stringResource(R.string.search_clear_content_description),
        onClickTrailingIcon = if (showClear) onClear else null,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
    )
}

@Preview(name = "SearchBar", showBackground = true)
@Composable
private fun SearchBarPreview() {
    AlMahirTheme {
        SearchBar(
            query = "",
            onQueryChange = {},
            hint = "Search by booking number or doctor name",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "SearchBar – RTL", showBackground = true)
@Composable
private fun SearchBarRtlPreview() {
    AlMahirTheme(locale = Locale("ar")) {
        SearchBar(
            query = "",
            onQueryChange = {},
            hint = "بحث برقم الاستشارة او اسم المستشار",
            modifier = Modifier.padding(16.dp),
        )
    }
}
