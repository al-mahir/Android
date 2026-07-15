package com.example.designsystem.components.search

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        text = query,
        onTextChange = onQueryChange,
        modifier = modifier,
        hint = hint,
        leadingIcon = painterResource(R.drawable.ic_search),
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
