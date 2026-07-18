package com.example.designsystem.components.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.designsystem.R
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

/**
 * Where the [iconPainter] sits relative to the caption inside a button. [Start] is the
 * default (matches Material `Button` with a leading icon — visual left in LTR, visual
 * right in RTL). [End] flips the source order so the icon is the row's last child —
 * visual right in LTR, visual left in RTL — used for "go forward" / "share" CTAs.
 */
enum class ButtonIconPosition { Start, End }

@Composable
internal fun BaseButton(
    caption: String?,
    modifier: Modifier = Modifier,
    iconPainter: Painter? = null,
    iconPosition: ButtonIconPosition = ButtonIconPosition.Start,
    containerColor: Color = Theme.colors.primary,
    contentColor: Color = Theme.colors.onPrimary,
    onClick: () -> Unit,
    isDisabled: Boolean = false,
    borderColor: Color = Color.Transparent,
    hasBorder: Boolean = false,
    isLoading: Boolean = false,
    tintIcon: Boolean = true,
    loadingView: (@Composable () -> Unit)? = null,
) {
    val backGroundColor = if (isDisabled) Theme.colors.disable else containerColor
    val borderColor = if (!hasBorder || isDisabled) Color.Transparent else borderColor
    Row(
        modifier = modifier
            .height(
                58.dp
            )
            .border(
                width = 1.dp,
                shape = Theme.shapes.small,
                color = borderColor
            )
            .clip(Theme.shapes.small)
            .background(backGroundColor)
            .clickable(
                enabled = !isDisabled && !isLoading,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            loadingView?.invoke()
        } else {
            val iconBlock: @Composable () -> Unit = {
                if (iconPainter != null) {
                    Image(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = if (tintIcon) {
                            ColorFilter.tint(if (isDisabled) Theme.colors.onDisable else contentColor)
                        } else null
                    )
                }
            }
            val captionBlock: @Composable () -> Unit = {
                if (caption != null) {
                    BasicText(
                        text = caption,
                        style = Theme.typography.body.large.copy(
                            color = if (isDisabled) Theme.colors.onDisable else contentColor
                        ),
                    )
                }
            }
            val gap: @Composable () -> Unit = {
                if (caption != null && iconPainter != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            when (iconPosition) {
                ButtonIconPosition.Start -> {
                    iconBlock(); gap(); captionBlock()
                }
                ButtonIconPosition.End -> {
                    captionBlock(); gap(); iconBlock()
                }
            }
        }
    }
}

//preview section

// preview primary Button
@Preview(
    showBackground = true,
)
@Composable
fun PreviewBaseButton() {
    AlMahirTheme(
        isDarkTheme = false,
        locale = Locale("ar")
    ) {
        Column (
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.colors.backGround),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            BaseButton(
                caption = stringResource(R.string.button),
                onClick = {},
                modifier = Modifier.fillMaxWidth(0.5f)
            )
            BaseButton(
                caption = stringResource(R.string.button),
                onClick = {},
                modifier = Modifier.fillMaxWidth(0.5f),
                isDisabled = true
            )
            BaseButton(
                caption = stringResource(R.string.button),
                onClick = {},
                modifier = Modifier.fillMaxWidth(0.5f),
                isLoading = true,
                loadingView = {

                }
            )
        }
    }
}

