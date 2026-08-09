package com.example.designsystem.components.selection

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.AlMahirTheme
import java.util.Locale

@Preview(name = "SelectableIconCardGrid – Light LTR", showBackground = true, group = "SelectableIconCard")
@Composable
private fun SelectableIconCardGridLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) { SelectableIconCardGridSample() }
}

@Preview(name = "SelectableIconCardGrid – Light RTL", showBackground = true, group = "SelectableIconCard")
@Composable
private fun SelectableIconCardGridLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) { SelectableIconCardGridSample() }
}

@Preview(name = "SelectableIconCardGrid – Dark LTR", showBackground = true, group = "SelectableIconCard")
@Composable
private fun SelectableIconCardGridDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) { SelectableIconCardGridSample() }
}

@Preview(name = "SelectableIconCardGrid – Dark RTL", showBackground = true, group = "SelectableIconCard")
@Composable
private fun SelectableIconCardGridDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) { SelectableIconCardGridSample() }
}

@Composable
private fun SelectableIconCardGridSample() {
    var selectedId by remember { mutableStateOf("vodafone") }
    val badgeColor = com.example.designsystem.theme.Theme.colors.surfaceVariant
    val items = listOf(
        SelectableIconCardItem(id = "vodafone", label = "Vodafone Cash", iconPainter = painterResource(R.drawable.ic_check), iconBackgroundColor = badgeColor),
        SelectableIconCardItem(id = "orange", label = "Orange Cash", iconPainter = painterResource(R.drawable.ic_check), iconBackgroundColor = badgeColor),
        SelectableIconCardItem(id = "etisalat", label = "e& money", iconPainter = painterResource(R.drawable.ic_check), iconBackgroundColor = badgeColor),
        SelectableIconCardItem(id = "we", label = "We Pay", iconPainter = painterResource(R.drawable.ic_check), iconBackgroundColor = badgeColor),
    )
    SelectableIconCardGrid(
        items = items,
        selectedId = selectedId,
        onItemSelected = { selectedId = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
}
