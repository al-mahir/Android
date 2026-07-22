package com.example.designsystem.color

import androidx.compose.ui.graphics.Color

/**
 * QUL-standard Tajweed color palette.
 * Each [TajweedRule] pairs an Arabic rule name with its canonical display color.
 */
data class TajweedRule(
    val nameArabic: String,
    val nameEnglish: String,
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
        TajweedRule("مَدّ لَازِم",          "Madd Lazim (Obligatory)",         madd),
        TajweedRule("مَدّ عَارِض / صِلَة",  "Madd 'Arid / Silah",              Color(0xFFFF6666)),
        TajweedRule("غُنَّة",               "Ghunnah (Nasalization)",           ghunnah),
        TajweedRule("إِخفَاء",              "Ikhfa' (Concealment)",             ikhfaa),
        TajweedRule("إِدغَام بِغُنَّة",     "Idgham with Ghunnah",             idghamBiGhunnah),
        TajweedRule("إِدغَام بِلَا غُنَّة", "Idgham without Ghunnah",          idghamBilaGhunnah),
        TajweedRule("إِقلَاب",              "Iqlab (Conversion)",               iqlab),
        TajweedRule("قَلقَلَة",             "Qalqalah (Echo)",                  qalqalah),
        TajweedRule("تَفخِيم",              "Tafkhim (Heaviness)",             tafkhim),
        TajweedRule("لَام شَمسِيَّة",       "Lam Shamsiyyah (Solar Lam)",      laamShamsia),
        TajweedRule("هَمزَة الوَصل",        "Hamzat Al-Wasl (Connecting Hamza)", hamzatulWasl),
    )
}
