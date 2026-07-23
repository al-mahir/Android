package com.example.mushaf.domain.model



 
data class Reciter(
    val id: Int,
    val name: String,
    val nameArabic: String,
    val style: RecitationStyle,
    val audioBaseUrl: String,
)
