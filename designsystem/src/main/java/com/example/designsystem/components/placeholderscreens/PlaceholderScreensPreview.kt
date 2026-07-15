package com.example.designsystem.components.placeholderscreens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.designsystem.theme.AlMahirTheme
import java.util.Locale

// ─── Combined showcase (Light/Dark × LTR/RTL) ────────────────────────────────

@Preview(name = "🖥 Placeholders Showcase – Light", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewPlaceholdersShowcaseLight() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        PlaceholderScreensShowcaseScreen()
    }
}

@Preview(name = "🖥 Placeholders Showcase – Light RTL", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewPlaceholdersShowcaseLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        PlaceholderScreensShowcaseScreen()
    }
}

@Preview(name = "🖥 Placeholders Showcase – Dark", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewPlaceholdersShowcaseDark() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) {
        PlaceholderScreensShowcaseScreen()
    }
}

@Preview(name = "🖥 Placeholders Showcase – Dark RTL", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewPlaceholdersShowcaseDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        PlaceholderScreensShowcaseScreen()
    }
}

// ─── Network Error ───────────────────────────────────────────────────────────

@Preview(name = "NetworkError – Light LTR", showBackground = true, group = "NetworkError", showSystemUi = true)
@Composable
private fun PreviewNetworkErrorLight() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        NetworkErrorScreen(onRetry = {})
    }
}

@Preview(name = "NetworkError – Dark RTL", showBackground = true, group = "NetworkError", showSystemUi = true)
@Composable
private fun PreviewNetworkErrorDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        NetworkErrorScreen(onRetry = {})
    }
}

@Preview(name = "NetworkError – No Action", showBackground = true, group = "NetworkError", showSystemUi = true)
@Composable
private fun PreviewNetworkErrorNoAction() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        NetworkErrorScreen(onRetry = null)
    }
}

// ─── Empty Search ────────────────────────────────────────────────────────────

@Preview(name = "EmptySearch – Light LTR", showBackground = true, group = "EmptySearch", showSystemUi = true)
@Composable
private fun PreviewEmptySearchLight() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        EmptySearchScreen()
    }
}

@Preview(name = "EmptySearch – Dark RTL", showBackground = true, group = "EmptySearch", showSystemUi = true)
@Composable
private fun PreviewEmptySearchDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        EmptySearchScreen()
    }
}

@Preview(name = "EmptySearch – With Action", showBackground = true, group = "EmptySearch", showSystemUi = true)
@Composable
private fun PreviewEmptySearchWithAction() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        EmptySearchScreen(actionButtonText = "Clear search", onActionClick = {})
    }
}

// ─── Empty Data ──────────────────────────────────────────────────────────────

@Preview(name = "EmptyData – Light LTR", showBackground = true, group = "EmptyData", showSystemUi = true)
@Composable
private fun PreviewEmptyDataLight() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        EmptyDataScreen(actionButtonText = "Create new", onActionClick = {})
    }
}

@Preview(name = "EmptyData – Dark RTL", showBackground = true, group = "EmptyData", showSystemUi = true)
@Composable
private fun PreviewEmptyDataDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        EmptyDataScreen(actionButtonText = "Create new", onActionClick = {})
    }
}

@Preview(name = "EmptyData – No Action", showBackground = true, group = "EmptyData", showSystemUi = true)
@Composable
private fun PreviewEmptyDataNoAction() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        EmptyDataScreen()
    }
}
