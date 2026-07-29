package com.example.designsystem.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale


@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = Theme.typography.body.small,
        color = Theme.colors.secondaryFont,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.field)
            .padding(
                horizontal = Theme.spacing.medium,
                vertical = Theme.spacing.small,
            ),
    )
}


@Composable
fun SettingsItemRow(
    title: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    contentColor: Color = Theme.colors.primaryFont,
    iconTint: Color = Theme.colors.secondaryFont,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.surface)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .semantics { role = Role.Button }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small)
            .heightIn(min = RowMinHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(Theme.size.iconMedium),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Theme.typography.body.large,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = Theme.typography.body.small,
                    color = Theme.colors.hint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        trailingContent()
    }
}


@Composable
fun SettingsChevron(
    modifier: Modifier = Modifier,
    tint: Color = Theme.colors.hint,
) {
    val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current ==
        androidx.compose.ui.unit.LayoutDirection.Rtl

    Icon(
        painter = painterResource(R.drawable.ic_chevron_end),
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(Theme.size.iconSemiMedium)
    )
}

@Composable
fun SettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Theme.colors.onPrimary,
            checkedTrackColor = Theme.colors.primary,
            checkedBorderColor = Theme.colors.primary,
            uncheckedThumbColor = Theme.colors.surface,
            uncheckedTrackColor = Theme.colors.disable,
            uncheckedBorderColor = Theme.colors.border,
        ),
        modifier = modifier,
    )
}

private val RowMinHeight: Dp = 40.dp

@Preview(name = "Settings rows RTL", locale = "ar", showBackground = true)
@Composable
private fun SettingsRowRtlPreview() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        SettingsRowPreviewContent()
    }
}

@Preview(name = "Settings rows LTR", locale = "en", showBackground = true)
@Composable
private fun SettingsRowLtrPreview() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("en")) {
        SettingsRowPreviewContent()
    }
}

@Composable
private fun SettingsRowPreviewContent() {
    Column(modifier = Modifier.background(Theme.colors.backGround)) {
        SettingsSectionHeader(title = "مظهر التطبيق")

        SettingsItemRow(
            title = "اللغة",
            icon = painterResource(R.drawable.ic_globe),
            onClick = {},
            trailingContent = { SettingsChevron() },
        )

        SettingsItemRow(
            title = "تذكيرات",
            icon = painterResource(R.drawable.ic_notifications),
            trailingContent = { SettingsSwitch(checked = true, onCheckedChange = {}) },
        )

        SettingsItemRow(
            title = "حذف جميع التسجيلات",
            icon = painterResource(R.drawable.ic_minus_circle),
            contentColor = Theme.colors.error,
            iconTint = Theme.colors.error,
            onClick = {},
            trailingContent = { SettingsChevron() },
        )

        Box(modifier = Modifier.size(Theme.spacing.medium))
    }
}
