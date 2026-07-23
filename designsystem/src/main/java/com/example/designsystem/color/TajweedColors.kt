package com.example.designsystem.color

import androidx.compose.ui.graphics.Color

/**
 * QUL-standard Tajweed color palette.
 * Each [TajweedRule] pairs an Arabic rule name with its canonical display color.
 */
data class TajweedRule(
    val nameArabic: String,
    val nameEnglish: String,
    val descriptionArabic: String,
    val color: Color,
)

object TajweedColors {

    // Primary rule colors based on the QUL Tajweed color system
    val madd          = Color(0xFFFF0000)   // Red — مد (prolongation)
    val ghunnah       = Color(0xFF008000)   // Green — غنة (nasalization)
    val ikhfaa        = Color(0xFF3D85C8)   // Blue — إخفاء (concealment)
    val idghamBiGhunnah = Color(0xFF006400) // Dark Green — إدغام بغنة
    val idghamBilaGhunnah = Color(0xFF8B4513) // Brown — إدغام بلا غنة
    val iqlab         = Color(0xFF800080)   // Purple — إقلاب
    val qalqalah      = Color(0xFF008B8B)   // Teal — قلقلة
    val tafkhim       = Color(0xFFFF8C00)   // Dark Orange — تفخيم
    val laamShamsia   = Color(0xFF696969)   // Gray — لام شمسية (solar lam)
    val hamzatulWasl  = Color(0xFF9400D3)   // Violet — همزة الوصل

    /** Ordered list for display in the legend sheet. */
    val rules: List<TajweedRule> = listOf(
        TajweedRule("مَدّ لَازِم",          "Madd Lazim (Obligatory)",         "6 حركات", madd),
        TajweedRule("مَدّ عَارِض / صِلَة",  "Madd 'Arid / Silah",              "2 أو 4 أو 6 حركات", Color(0xFFFF6666)),
        TajweedRule("غُنَّة",               "Ghunnah (Nasalization)",           "حركتان", ghunnah),
        TajweedRule("إِخفَاء",              "Ikhfa' (Concealment)",             "إخفاء مع الغنة مقدار حركتين", ikhfaa),
        TajweedRule("إِدغَام بِغُنَّة",     "Idgham with Ghunnah",             "إدغام بغنة حركتين", idghamBiGhunnah),
        TajweedRule("إِدغَام بِلَا غُنَّة", "Idgham without Ghunnah",          "إدغام كامل بدون غنة", idghamBilaGhunnah),
        TajweedRule("إِقلَاب",              "Iqlab (Conversion)",               "قلب النون ميماً مع الغنة", iqlab),
        TajweedRule("قَلقَلَة",             "Qalqalah (Echo)",                  "اهتزاز الصوت بالحرف الساكن", qalqalah),
        TajweedRule("تَفخِيم",              "Tafkhim (Heaviness)",             "تغليظ الحرف", tafkhim),
        TajweedRule("لَام شَمسِيَّة",       "Lam Shamsiyyah (Solar Lam)",      "تُدغم ولا تُلفظ", laamShamsia),
        TajweedRule("هَمزَة الوَصل",        "Hamzat Al-Wasl (Connecting Hamza)", "تُكتب وتسقط وصلاً", hamzatulWasl),
    )
}
