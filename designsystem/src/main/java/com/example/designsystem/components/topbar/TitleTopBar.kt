package com.example.designsystem.components.topbar

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

/**
 * Top bar showing only a centred title — no leading or trailing controls. Use for
 * bottom-nav top-level screens (e.g. Bookings) where there is no back action and no
 * notification icon. The [decoration] slot accepts custom background art identical to
 * the other top-bar variants.
 */
@Composable
fun TitleTopBar(
    title: String,
    modifier: Modifier = Modifier,
    decoration: (@Composable BoxScope.() -> Unit)? = null,
) {
    BaseTopBar(
        modifier = modifier,
        decoration = decoration,
        center = {
            BasicText(
                text = title,
                style = Theme.typography.title.copy(
                    color = Theme.colors.onPrimary,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
            )
        },
    )
}

@Preview(name = "TitleTopBar – RTL", showBackground = true)
@Composable
private fun TitleTopBarPreview() {
    AlMahirTheme(locale = Locale("ar")) {
        TitleTopBar(title = "حجوزاتي")
    }
}
