package com.example.designsystem.components.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.components.preview.PreviewHelperComposable
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

// ─── Single showcase preview (run on device / emulator via Deploy Preview) ───

@Preview(name = "🖥 All Buttons Showcase – Light", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewAllButtonsShowcaseLight() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        ButtonShowcaseScreen()
    }
}

@Preview(name = "🖥 All Buttons Showcase – Dark RTL", showBackground = true, group = "Showcase", showSystemUi = true)
@Composable
private fun PreviewAllButtonsShowcaseDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        ButtonShowcaseScreen()
    }
}

// ─── Primary Button ───────────────────────────────────────────────────────────

@Preview(name = "PrimaryButton – Light LTR", showBackground = true, group = "PrimaryButton")
@Composable
private fun PreviewPrimaryButtonLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                PrimaryButtonAllStates()
            }
        }
    }
}

@Preview(name = "PrimaryButton – Light RTL", showBackground = true, group = "PrimaryButton")
@Composable
private fun PreviewPrimaryButtonLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                PrimaryButtonAllStates()
            }
        }
    }
}

@Preview(name = "PrimaryButton – Dark LTR", showBackground = true, group = "PrimaryButton")
@Composable
private fun PreviewPrimaryButtonDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                PrimaryButtonAllStates()
            }
        }
    }
}

@Preview(name = "PrimaryButton – Dark RTL", showBackground = true, group = "PrimaryButton")
@Composable
private fun PreviewPrimaryButtonDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                PrimaryButtonAllStates()
            }
        }
    }
}

// ─── Secondary Button ─────────────────────────────────────────────────────────

@Preview(name = "SecondaryButton – Light LTR", showBackground = true, group = "SecondaryButton")
@Composable
private fun PreviewSecondaryButtonLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                SecondaryButtonAllStates()
            }
        }
    }
}

@Preview(name = "SecondaryButton – Light RTL", showBackground = true, group = "SecondaryButton")
@Composable
private fun PreviewSecondaryButtonLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                SecondaryButtonAllStates()
            }
        }
    }
}

@Preview(name = "SecondaryButton – Dark LTR", showBackground = true, group = "SecondaryButton")
@Composable
private fun PreviewSecondaryButtonDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                SecondaryButtonAllStates()
            }
        }
    }
}

@Preview(name = "SecondaryButton – Dark RTL", showBackground = true, group = "SecondaryButton")
@Composable
private fun PreviewSecondaryButtonDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                SecondaryButtonAllStates()
            }
        }
    }
}

// ─── Icon Button ──────────────────────────────────────────────────────────────

@Preview(name = "IconButton – Light LTR", showBackground = true, group = "IconButton")
@Composable
private fun PreviewIconButtonLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                IconButtonAllStates()
            }
        }
    }
}

@Preview(name = "IconButton – Light RTL", showBackground = true, group = "IconButton")
@Composable
private fun PreviewIconButtonLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                IconButtonAllStates()
            }
        }
    }
}

@Preview(name = "IconButton – Dark LTR", showBackground = true, group = "IconButton")
@Composable
private fun PreviewIconButtonDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale.ENGLISH) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                IconButtonAllStates()
            }
        }
    }
}

@Preview(name = "IconButton – Dark RTL", showBackground = true, group = "IconButton")
@Composable
private fun PreviewIconButtonDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                IconButtonAllStates()
            }
        }
    }
}

// ─── All Buttons Combined ─────────────────────────────────────────────────────

@Preview(name = "All Buttons – Light LTR", showBackground = true, group = "AllButtons")
@Composable
private fun PreviewAllButtonsLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                SectionLabel("Primary Button")
                PrimaryButtonAllStates()
                Spacer(Modifier.height(8.dp))
                SectionLabel("Secondary Button")
                SecondaryButtonAllStates()
                Spacer(Modifier.height(8.dp))
                SectionLabel("Icon Button")
                IconButtonAllStates()
            }
        }
    }
}

@Preview(name = "All Buttons – Dark RTL", showBackground = true, group = "AllButtons")
@Composable
private fun PreviewAllButtonsDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                SectionLabel("Primary Button")
                PrimaryButtonAllStates()
                Spacer(Modifier.height(8.dp))
                SectionLabel("Secondary Button")
                SecondaryButtonAllStates()
                Spacer(Modifier.height(8.dp))
                SectionLabel("Icon Button")
                IconButtonAllStates()
            }
        }
    }
}

// ─── Button with Icon ─────────────────────────────────────────────────────────

@Preview(name = "Button with Icon – Light", showBackground = true, group = "ButtonWithIcon")
@Composable
private fun PreviewButtonWithIconLight() {
    AlMahirTheme(isDarkTheme = false, locale = Locale.ENGLISH) {
        PreviewHelperComposable {
            ButtonStatesColumn {
                SectionLabel("Primary + Icon")
                PrimaryButton(
                    caption = stringResource(R.string.button),
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    iconPainter = painterResource(id = R.drawable.ic_finger_print),
                )
                PrimaryButton(
                    caption = stringResource(R.string.button),
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    iconPainter = painterResource(id = R.drawable.ic_finger_print),
                    isDisabled = true,
                )
                Spacer(Modifier.height(8.dp))
                SectionLabel("Secondary + Icon")
                SecondaryButton(
                    caption = stringResource(R.string.button),
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    iconPainter = painterResource(id = R.drawable.ic_finger_print),
                )
                SecondaryButton(
                    caption = stringResource(R.string.button),
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    iconPainter = painterResource(id = R.drawable.ic_finger_print),
                    isDisabled = true,
                )
            }
        }
    }
}

// ─── Reusable state sets ──────────────────────────────────────────────────────

@Composable
private fun PrimaryButtonAllStates() {
    StateRow(label = "Normal") {
        PrimaryButton(
            caption = stringResource(R.string.button),
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
    StateRow(label = "Disabled") {
        PrimaryButton(
            caption = stringResource(R.string.button),
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            isDisabled = true,
        )
    }
    StateRow(label = "Loading") {
        PrimaryButton(
            caption = stringResource(R.string.button),
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            isLoading = true,
        )
    }
}

@Composable
private fun SecondaryButtonAllStates() {
    StateRow(label = "Normal") {
        SecondaryButton(
            caption = stringResource(R.string.button),
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
    StateRow(label = "Disabled") {
        SecondaryButton(
            caption = stringResource(R.string.button),
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            isDisabled = true,
        )
    }
    StateRow(label = "Loading") {
        SecondaryButton(
            caption = stringResource(R.string.button),
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            isLoading = true,
        )
    }
}

@Composable
private fun IconButtonAllStates() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = {},
                icon = painterResource(id = R.drawable.ic_finger_print),
            )
            Spacer(Modifier.height(4.dp))
            StateLabel("Normal")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = {},
                icon = painterResource(id = R.drawable.ic_finger_print),
                isDisabled = true,
            )
            Spacer(Modifier.height(4.dp))
            StateLabel("Disabled")
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun ButtonStatesColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun StateRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(
            text = label,
            style = Theme.typography.body.small.copy(color = Theme.colors.hint),
            modifier = Modifier.fillMaxWidth(0.2f),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    BasicText(
        text = text,
        style = Theme.typography.body.large.copy(color = Theme.colors.secondary),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StateLabel(text: String) {
    BasicText(
        text = text,
        style = Theme.typography.body.small.copy(color = Theme.colors.hint),
    )
}


