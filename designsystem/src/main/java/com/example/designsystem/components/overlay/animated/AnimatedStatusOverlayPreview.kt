package com.example.designsystem.components.overlay.animated

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.designsystem.theme.AlMahirTheme
import java.util.Locale

// ─── Single showcase preview (run on device / emulator via Deploy Preview) ───
//
// Lottie animations and Dialog overlays do not render in Android Studio's
// static preview pane — these previews must be deployed to a device/emulator
// to be exercised. Tap the buttons in the showcase to trigger each scenario.

@Preview(name = "🖥 Status Overlay Showcase – Light", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewAnimatedStatusOverlayShowcaseLight() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        AnimatedStatusOverlayShowcaseScreen()
    }
}

@Preview(name = "🖥 Status Overlay Showcase – Light RTL", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewAnimatedStatusOverlayShowcaseLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        AnimatedStatusOverlayShowcaseScreen()
    }
}

@Preview(name = "🖥 Status Overlay Showcase – Dark", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewAnimatedStatusOverlayShowcaseDark() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) {
        AnimatedStatusOverlayShowcaseScreen()
    }
}

@Preview(name = "🖥 Status Overlay Showcase – Dark RTL", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewAnimatedStatusOverlayShowcaseDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        AnimatedStatusOverlayShowcaseScreen()
    }
}
