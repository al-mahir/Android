package com.example.designsystem.components.textfield

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.example.designsystem.theme.AlMahirTheme
import java.util.Locale

// ─── Single showcase preview (run on device / emulator via Deploy Preview) ───

@Preview(name = "🖥 All TextFields Showcase – Light", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewAllTextFieldsShowcaseLight() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        TextFieldShowcaseScreen()
    }
}

@Preview(name = "🖥 All TextFields Showcase – Light RTL", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewAllTextFieldsShowcaseLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        TextFieldShowcaseScreen()
    }
}

@Preview(name = "🖥 All TextFields Showcase – Dark", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewAllTextFieldsShowcaseDark() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) {
        TextFieldShowcaseScreen()
    }
}

@Preview(name = "🖥 All TextFields Showcase – Dark RTL", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewAllTextFieldsShowcaseDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        TextFieldShowcaseScreen()
    }
}

