package com.iti.data.mapper

private val HONORIFICS = setOf(
    "الشيخ",
    "الشيخة",
    "الأستاذ",
    "الأستاذة",
    "الدكتور",
    "الدكتورة",
)


internal fun String.toInitials(): String {
    val words = trim()
        .split(' ', '\u00A0')
        .filter { it.isNotBlank() }
        .filterNot { it in HONORIFICS }

    return when (words.size) {
        0 -> ""
        1 -> words.first().take(2)
        else -> words.take(2).mapNotNull { word -> word.firstOrNull() }.joinToString(separator = "")
    }
}
