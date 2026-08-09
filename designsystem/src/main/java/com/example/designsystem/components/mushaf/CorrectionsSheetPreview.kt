package com.example.designsystem.components.mushaf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.designsystem.theme.AlMahirTheme
import com.example.designsystem.theme.Theme
import java.util.Locale

private val sampleCorrections = listOf(
    CorrectionCardUi(
        id = "1:3",
        ayahLabel = "١:٣",
        words = listOf(
            CorrectionWordUi("ٱلْحَمْدُ", isMistake = true),
            CorrectionWordUi("لِلَّهِ", isMistake = true),
            CorrectionWordUi("ٱلرَّحْمَٰنِ", isMistake = false),
            CorrectionWordUi("ٱلرَّحِيمِ", isMistake = false),
        ),
        mistakes = listOf(
            CorrectionMistakeUi(
                "1:3:1", "ٱلْحَمْدُ",
                listOf(CorrectionFindingUi("كلمات زائدة")),
            ),
            // Two findings on one word — the case the old single-label row could not show.
            CorrectionMistakeUi(
                "1:3:2", "لِلَّهِ",
                listOf(
                    CorrectionFindingUi(
                        "خطأ في التشكيل",
                        listOf("المتوقع «lilla:hi»، ونطقت «lilla:ha»", "نسبة الثقة ٨٢٪"),
                    ),
                    CorrectionFindingUi("خطأ في التجويد", listOf("الحكم: الإدغام")),
                ),
            ),
        ),
    ),
    CorrectionCardUi(
        id = "1:7",
        ayahLabel = "١:٧",
        words = listOf(
            CorrectionWordUi("صِرَٰطَ", isMistake = false),
            CorrectionWordUi("ٱلَّذِينَ", isMistake = false),
            CorrectionWordUi("أَنْعَمْتَ", isMistake = true),
            CorrectionWordUi("عَلَيْهِمْ", isMistake = false),
        ),
        mistakes = listOf(
            CorrectionMistakeUi(
                "1:7:3", "أَنْعَمْتَ",
                listOf(
                    CorrectionFindingUi(
                        "خطأ في التجويد",
                        listOf(
                            "المد الطبيعي: المتوقع ٢، قرأت ٣",
                            "المتوقع «ʔanʕamta»، ونطقت «ʔanʕaːmta»",
                            "نسبة الثقة ٩١٪",
                        ),
                    ),
                ),
            ),
        ),
    ),
)

/**
 * The sheet's body on a plain surface. `ModalBottomSheet` does not render on the preview
 * surface, so the list is previewed directly — it is the layout worth reviewing.
 */
@Composable
private fun CorrectionsListPreviewBody(corrections: List<CorrectionCardUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.backGround)
            .padding(horizontal = Theme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        CorrectionsList(
            title = "التصحيحات",
            subtitle = "٣ أخطاء · الفاتحة ١ — الفاتحة ٧",
            corrections = corrections,
            emptyMessage = "لا توجد أخطاء بعد",
        )
    }
}

@Preview(name = "Corrections · Light RTL", locale = "ar")
@Composable
private fun PreviewCorrectionsLightRtl() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        CorrectionsListPreviewBody(sampleCorrections)
    }
}

@Preview(name = "Corrections · Dark RTL", locale = "ar")
@Composable
private fun PreviewCorrectionsDarkRtl() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("ar")) {
        CorrectionsListPreviewBody(sampleCorrections)
    }
}

@Preview(name = "Corrections · Light LTR", locale = "en")
@Composable
private fun PreviewCorrectionsLightLtr() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("en")) {
        CorrectionsListPreviewBody(
            listOf(
                sampleCorrections[1].copy(
                    mistakes = listOf(
                        CorrectionMistakeUi(
                            "1:7:3", "أَنْعَمْتَ",
                            listOf(
                                CorrectionFindingUi(
                                    "Tajweed mistake",
                                    listOf(
                                        "Normal Madd: expected 2, you held 3",
                                        "Expected “ʔanʕamta”, you said “ʔanʕaːmta”",
                                        "Confidence 91%",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}

@Preview(name = "Corrections · Dark LTR", locale = "en")
@Composable
private fun PreviewCorrectionsDarkLtr() {
    AlMahirTheme(isDarkTheme = true, locale = Locale("en")) {
        CorrectionsListPreviewBody(sampleCorrections)
    }
}

@Preview(name = "Corrections badge", locale = "ar")
@Composable
private fun PreviewCorrectionsBadge() {
    AlMahirTheme(isDarkTheme = false, locale = Locale("ar")) {
        Column(
            modifier = Modifier.background(Theme.colors.backGround).padding(Theme.spacing.medium),
        ) {
            CorrectionsBadge(count = 2, contentDescription = "مراجعة الأخطاء", onClick = {})
        }
    }
}
