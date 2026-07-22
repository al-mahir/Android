package com.example.designsystem.components.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale


@Composable
fun PlainTitleTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    backIcon: Painter = painterResource(R.drawable.ic_chevron_end),
    end: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround)
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small)
            .height(PlainTopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = Theme.typography.title.copy(fontWeight = FontWeight.Bold),
            color = Theme.colors.primaryFont,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            end?.invoke(this)

            Box(
                modifier = Modifier
                    .size(BackButtonSize)
                    .border(width = 1.dp, color = Theme.colors.border, shape = CircleShape)
                    .clickable(onClick = onBackClick)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = backIcon,
                    contentDescription = stringResource(
                        R.string.topbar_back_content_description,
                    ),
                    tint = Theme.colors.secondaryFont,
                    modifier = Modifier.size(Theme.size.iconSemiMedium),
                )
            }
        }
    }
}

private val PlainTopBarHeight: Dp = 48.dp
private val BackButtonSize: Dp = 36.dp

@Preview(name = "Plain top bar RTL", locale = "ar", showBackground = true)
@Composable
private fun PlainTitleTopBarRtlPreview() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        PlainTitleTopBar(title = "الإعدادات", onBackClick = {})
    }
}

@Preview(name = "Plain top bar LTR", locale = "en", showBackground = true)
@Composable
private fun PlainTitleTopBarLtrPreview() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("en")) {
        PlainTitleTopBar(title = "Settings", onBackClick = {})
    }
}
