package com.example.designsystem.components.bottomsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.locale.rememberLocaleLocals
import com.example.designsystem.theme.Theme

/**
 * Thin wrapper around Material3 [ModalBottomSheet] that wires up the design-system theme
 * tokens (surface color, top-rounded shape) so feature code imports this instead of M3
 * directly and the bottom-sheet visual stays consistent.
 *
 * Shape: a top-only rounded corner — sheets dock to the bottom edge of the screen, so
 * rounding the bottom corners (what `Theme.shapes.large` does) makes the sheet look
 * like a floating dialog instead. We override here.
 *
 * Locale propagation: ModalBottomSheet renders in a separate Popup window whose owner
 * view re-overrides `LocalContext`/`LocalConfiguration`/`LocalLayoutDirection` with the
 * activity defaults. That bypasses the localized context AlMahirTheme installs at the root,
 * so `stringResource` inside the sheet would resolve against the system locale. We capture
 * the parent locale-aware locals up here and re-provide them inside the sheet — see
 * [com.example.core.designsystem.locale.LocaleLocals], which every popup-hosted composable
 * needs, not just this one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    val localeLocals = rememberLocaleLocals()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Theme.colors.backGround,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        localeLocals.Provide {
            Box(modifier = Modifier.padding(horizontal = Theme.spacing.medium)) {
                Column(content = content)
            }
        }
    }
}
