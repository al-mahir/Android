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

    // Primary rule colors based on Tarteel's official system
    val maddLazim      = Color(0xFFB71C1C) // Dark Red
    val maddMuttasil   = Color(0xFFFF5722) // Orange-Red
    val maddArid       = Color(0xFFFF9800) // Light Orange
    val maddTabee      = Color(0xFFFFC107) // Yellow / Amber
    val ghunnah        = Color(0xFF4CAF50) // Green
    val qalqalah       = Color(0xFF29B6F6) // Light Blue
    val tafkhim        = Color(0xFF283593) // Dark Blue
    val silentLetter   = Color(0xFF9E9E9E) // Gray

    /** Ordered list for display in the legend sheet. */
    val rules: List<TajweedRule> = listOf(
        TajweedRule(
            nameArabic = "مَدّ لَازِم",
            nameEnglish = "Madd Lazim",
            descriptionArabic = "6 حركات",
            color = maddLazim
        ),
        TajweedRule(
            nameArabic = "مَدّ وَاجِب مُتَّصِل",
            nameEnglish = "Madd Wajib Muttasil",
            descriptionArabic = "4 - 5 حركات",
            color = maddMuttasil
        ),
        TajweedRule(
            nameArabic = "مَدّ عَارِض لِلسُّكُون",
            nameEnglish = "Madd 'Arid",
            descriptionArabic = "2، 4، أو 6 حركات",
            color = maddArid
        ),
        TajweedRule(
            nameArabic = "مَدّ طَبِيعِي",
            nameEnglish = "Madd Tabi'i",
            descriptionArabic = "حركتان",
            color = maddTabee
        ),
        TajweedRule(
            nameArabic = "غُنَّة",
            nameEnglish = "Ghunnah",
            descriptionArabic = "نون وميم مشددتين",
            color = ghunnah
        ),
        TajweedRule(
            nameArabic = "قَلقَلَة",
            nameEnglish = "Qalqalah",
            descriptionArabic = "اهتزاز الصوت بالحرف الساكن",
            color = qalqalah
        ),
        TajweedRule(
            nameArabic = "تَفخِيم",
            nameEnglish = "Tafkhim",
            descriptionArabic = "تغليظ الحرف",
            color = tafkhim
        ),
        TajweedRule(
            nameArabic = "حُرُوف صَامِتَة",
            nameEnglish = "Silent Letters",
            descriptionArabic = "تُكتب ولا تُلفظ",
            color = silentLetter
        ),
    )
}
