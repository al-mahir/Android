package com.example.designsystem.components.permission

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.designsystem.theme.AlMahirTheme
import java.util.Locale

@Composable
private fun ArabicPreviewContent() {
    MicPermissionPreprompt(
        title = "نحتاج إلى الميكروفون",
        message = "لتصحيح تلاوتك مباشرةً، يستمع التطبيق إلى صوتك أثناء القراءة فقط.",
        confirmLabel = "متابعة",
        dismissLabel = "ليس الآن",
        reasons = listOf(
            "يبدأ التسجيل عند ضغط زر الميكروفون فقط",
            "لا يتم حفظ صوتك على الجهاز أو الخادم",
            "يمكنك إيقافه في أي وقت",
        ),
        onConfirm = {},
        onDismiss = {},
    )
}

@Composable
private fun EnglishPreviewContent() {
    MicPermissionPreprompt(
        title = "We need your microphone",
        message = "To correct your recitation live, the app listens only while you are reciting.",
        confirmLabel = "Continue",
        dismissLabel = "Not now",
        reasons = listOf(
            "Recording starts only when you tap the mic",
            "Your voice is not saved on the device or the server",
            "You can stop at any time",
        ),
        onConfirm = {},
        onDismiss = {},
    )
}

@Preview(name = "Mic pre-prompt · Light RTL", locale = "ar")
@Composable
private fun PreviewMicPrepromptLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) { ArabicPreviewContent() }
}

@Preview(name = "Mic pre-prompt · Dark RTL", locale = "ar")
@Composable
private fun PreviewMicPrepromptDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) { ArabicPreviewContent() }
}

@Preview(name = "Mic pre-prompt · Light LTR", locale = "en")
@Composable
private fun PreviewMicPrepromptLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("en")) { EnglishPreviewContent() }
}

@Preview(name = "Mic pre-prompt · Dark LTR", locale = "en")
@Composable
private fun PreviewMicPrepromptDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("en")) { EnglishPreviewContent() }
}
