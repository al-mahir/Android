package com.example.designsystem.components.bottomnav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

// ─── Showcase (deployable) ────────────────────────────────────────────────────

@Preview(name = "Showcase – Light", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewShowcaseLight() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        BottomNavShowcaseScreen()
    }
}

@Preview(name = "Showcase – Dark RTL", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewShowcaseDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        BottomNavShowcaseScreen()
    }
}

// ─── AppBottomNavBar – per destination ────────────────────────────────────────

@Preview(name = "AppBottomNavBar – Home, Light LTR", showBackground = true, group = "AppBottomNavBar")
@Composable
private fun PreviewAppHomeLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        BottomNavPreviewScaffold {
            StateLabel("Home selected")
            AppBottomNavBar(
                selectedDestination = AppBottomNavDestination.Home,
                onDestinationSelected = {},
            )
        }
    }
}

@Preview(name = "AppBottomNavBar – Mushaf, Light LTR", showBackground = true, group = "AppBottomNavBar")
@Composable
private fun PreviewAppMushafLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        BottomNavPreviewScaffold {
            StateLabel("Mushaf selected")
            AppBottomNavBar(
                selectedDestination = AppBottomNavDestination.Mushaf,
                onDestinationSelected = {},
            )
        }
    }
}

@Preview(name = "AppBottomNavBar – Profile, Light LTR", showBackground = true, group = "AppBottomNavBar")
@Composable
private fun PreviewAppProfileLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        BottomNavPreviewScaffold {
            StateLabel("Profile selected")
            AppBottomNavBar(
                selectedDestination = AppBottomNavDestination.Profile,
                onDestinationSelected = {},
            )
        }
    }
}

// ─── AppBottomNavBar – RTL ────────────────────────────────────────────────────

@Preview(name = "AppBottomNavBar – Home, Light RTL", showBackground = true, group = "AppBottomNavBar")
@Composable
private fun PreviewAppHomeLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        BottomNavPreviewScaffold {
            StateLabel("Home selected (RTL)")
            AppBottomNavBar(
                selectedDestination = AppBottomNavDestination.Home,
                onDestinationSelected = {},
            )
        }
    }
}

@Preview(name = "AppBottomNavBar – Mushaf, Light RTL", showBackground = true, group = "AppBottomNavBar")
@Composable
private fun PreviewAppMushafLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        BottomNavPreviewScaffold {
            StateLabel("Mushaf selected (RTL)")
            AppBottomNavBar(
                selectedDestination = AppBottomNavDestination.Mushaf,
                onDestinationSelected = {},
            )
        }
    }
}

// ─── AppBottomNavBar – Dark ───────────────────────────────────────────────────

@Preview(name = "AppBottomNavBar – Home, Dark LTR", showBackground = true, group = "AppBottomNavBar")
@Composable
private fun PreviewAppHomeDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) {
        BottomNavPreviewScaffold {
            StateLabel("Home selected (Dark)")
            AppBottomNavBar(
                selectedDestination = AppBottomNavDestination.Home,
                onDestinationSelected = {},
            )
        }
    }
}

@Preview(name = "AppBottomNavBar – Profile, Dark RTL", showBackground = true, group = "AppBottomNavBar")
@Composable
private fun PreviewAppProfileDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        BottomNavPreviewScaffold {
            StateLabel("Profile selected (Dark RTL)")
            AppBottomNavBar(
                selectedDestination = AppBottomNavDestination.Profile,
                onDestinationSelected = {},
            )
        }
    }
}

// ─── All destinations stacked ─────────────────────────────────────────────────

@Preview(name = "All destinations – Light LTR", showBackground = true, group = "AllDestinations")
@Composable
private fun PreviewAllDestinationsLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        BottomNavPreviewScaffold { AllDestinationsBlock() }
    }
}

@Preview(name = "All destinations – Dark RTL", showBackground = true, group = "AllDestinations")
@Composable
private fun PreviewAllDestinationsDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        BottomNavPreviewScaffold { AllDestinationsBlock() }
    }
}

// ─── Reusable state blocks ────────────────────────────────────────────────────

@Composable
private fun AllDestinationsBlock() {
    StateLabel("Home selected")
    AppBottomNavBar(
        selectedDestination = AppBottomNavDestination.Home,
        onDestinationSelected = {},
    )
    Spacer(Modifier.height(8.dp))
    StateLabel("Mushaf selected")
    AppBottomNavBar(
        selectedDestination = AppBottomNavDestination.Mushaf,
        onDestinationSelected = {},
    )
    Spacer(Modifier.height(8.dp))
    StateLabel("Profile selected")
    AppBottomNavBar(
        selectedDestination = AppBottomNavDestination.Profile,
        onDestinationSelected = {},
    )
}

// ─── Layout helpers ───────────────────────────────────────────────────────────

@Composable
private fun BottomNavPreviewScaffold(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.surface)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        content()
    }
}

@Composable
private fun StateLabel(text: String) {
    BasicText(
        text = text,
        style = Theme.typography.body.small.copy(color = Theme.colors.hint),
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
