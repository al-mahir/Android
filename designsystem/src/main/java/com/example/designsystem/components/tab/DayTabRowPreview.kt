package com.example.designsystem.components.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

@Preview(name = "DayTabRow – RTL", showBackground = true, group = "DayTabRow")
@Composable
private fun DayTabRowRtl() {
    AlMahirTheme(locale = Locale("ar")) { DayTabRowSample() }
}

@Preview(name = "DayTabRow – LTR", showBackground = true, group = "DayTabRow")
@Composable
private fun DayTabRowLtr() {
    AlMahirTheme(locale = Locale.ENGLISH) { DayTabRowSample() }
}

@Composable
private fun DayTabRowSample() {
    val days = listOf("سبت", "الاحد", "الاثنين", "الثلاثاء", "الاربعا", "الخميس", "الجمعة")
    var selected by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround)
            .padding(16.dp),
    ) {
        DayTabRow(labels = days, selectedIndex = selected, onSelect = { selected = it })
    }
}
