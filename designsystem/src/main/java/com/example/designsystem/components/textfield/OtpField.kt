package com.example.designsystem.components.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.designsystem.theme.Theme

/**
 * Auto-advancing one-time-code input rendered as [length] equal cells.
 *
 * Implemented as a single [BasicTextField] whose [BasicTextField.decorationBox] paints
 * the visible cells. The text field itself is the touch target, so tapping anywhere
 * along the row focuses the field and raises the soft keyboard. [onValueChange] is
 * only invoked with digit-strings up to [length] long; non-digits are filtered out.
 *
 * The "focused" cell is the position of the next empty slot (or the last cell once full).
 */
@Composable
fun OtpField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 4,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    onImeDone: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFieldFocused: Boolean by interactionSource.collectIsFocusedAsState()

    val sanitized = value.filter(Char::isDigit).take(length)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        BasicTextField(
            value = sanitized,
            onValueChange = { new ->
                val cleaned = new.filter(Char::isDigit).take(length)
                if (cleaned != sanitized) onValueChange(cleaned)
                if (cleaned.length == length) {
                    focusManager.clearFocus()
                    onImeDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onImeDone()
                },
            ),
            cursorBrush = SolidColor(Color.Transparent),
            textStyle = TextStyle(color = Color.Transparent),
            interactionSource = interactionSource,
            decorationBox = { _ ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    repeat(length) { index ->
                        val char = sanitized.getOrNull(index)
                        val isActive = isFieldFocused && index == sanitized.length.coerceAtMost(length - 1)
                        OtpCell(
                            char = char,
                            isActive = isActive,
                            isError = isError,
                            enabled = enabled,
                        )
                    }
                }
            },
        )

        if (isError && !errorMessage.isNullOrBlank()) {
            BasicText(
                text = errorMessage,
                style = Theme.typography.body.small.copy(color = Theme.colors.error),
            )
        }
    }
}

@Composable
private fun OtpCell(
    char: Char?,
    isActive: Boolean,
    isError: Boolean,
    enabled: Boolean,
) {
    val borderColor: Color = when {
        isError -> Theme.colors.error
        isActive -> Theme.colors.primary
        else -> Theme.colors.border
    }
    val container: Color = if (enabled) Theme.colors.field else Theme.colors.disable
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(Theme.shapes.medium)
            .background(container)
            .border(width = 1.dp, color = borderColor, shape = Theme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        if (char != null) {
            BasicText(
                text = char.toString(),
                style = Theme.typography.title.copy(
                    color = if (enabled) Theme.colors.primaryFont else Theme.colors.onDisable,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}
