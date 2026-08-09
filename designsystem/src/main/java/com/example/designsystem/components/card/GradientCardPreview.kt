package com.example.designsystem.components.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

@Preview(name = "GradientCard – Light LTR", showBackground = true, group = "GradientCard")
@Composable
private fun GradientCardLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) { GradientCardSample() }
}

@Preview(name = "GradientCard – Light RTL", showBackground = true, group = "GradientCard")
@Composable
private fun GradientCardLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) { GradientCardSample() }
}

@Preview(name = "GradientCard – Dark LTR", showBackground = true, group = "GradientCard")
@Composable
private fun GradientCardDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) { GradientCardSample() }
}

@Preview(name = "GradientCard – Dark RTL", showBackground = true, group = "GradientCard")
@Composable
private fun GradientCardDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) { GradientCardSample() }
}

@Composable
private fun GradientCardSample() {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        GradientCard {
            Text(text = "Reciter Pass", style = Theme.typography.body.medium)
            Text(text = "40.00 EGP", style = Theme.typography.title)
        }
    }
}
