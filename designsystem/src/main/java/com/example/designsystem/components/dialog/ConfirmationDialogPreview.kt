package com.example.designsystem.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

@Composable
private fun DestructivePreviewContent() {
    ConfirmationDialog(
        title = "إلغاء الحساب",
        message = "سيتم حذف حسابك وجميع تسجيلاتك نهائياً. لا يمكن التراجع عن هذا الإجراء.",
        confirmLabel = "حذف",
        dismissLabel = "إلغاء",
        confirmColor = Theme.colors.error,
        confirmContentColor = Theme.colors.onError,
        onConfirm = {},
        onDismiss = {},
    )
}

@Preview(name = "Destructive · Light RTL", locale = "ar")
@Composable
private fun PreviewConfirmationDialogLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) { DestructivePreviewContent() }
}

@Preview(name = "Destructive · Dark RTL", locale = "ar")
@Composable
private fun PreviewConfirmationDialogDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) { DestructivePreviewContent() }
}

@Preview(name = "Destructive · Light LTR", locale = "en")
@Composable
private fun PreviewConfirmationDialogLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("en")) { DestructivePreviewContent() }
}

@Preview(name = "Destructive · Dark LTR", locale = "en")
@Composable
private fun PreviewConfirmationDialogDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("en")) { DestructivePreviewContent() }
}
