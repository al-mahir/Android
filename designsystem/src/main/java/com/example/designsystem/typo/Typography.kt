package com.example.designsystem.typo

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class SizedTextStyle(
    val large: TextStyle,
    val medium: TextStyle,
    val small: TextStyle
)


internal fun spTypographyOf(fontFamily: FontFamily): SPTextStyle = SPTextStyle(
    display = TextStyle(
        fontFamily = fontFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 42.sp
    ),
    title = TextStyle(
        fontFamily = fontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp
    ),

    body = SizedTextStyle(
        large = TextStyle(
            fontFamily = fontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 24.sp
        ),
        medium = TextStyle(
            fontFamily = fontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 20.sp
        ),
        small = TextStyle(
            fontFamily = fontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.sp
        )
    ),
    hint = SizedTextStyle(
        large = TextStyle(
            fontFamily = fontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 26.sp
        ),
        medium = TextStyle(
            fontFamily = fontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 20.sp
        ),
        small = TextStyle(
            fontFamily = fontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 16.sp
        )
    )
)

internal fun defaultSPTypographyForLanguage(languageCode: String): SPTextStyle =
    spTypographyOf(fontFamily = if (languageCode == "ar") arabicFontFamily else arabicFontFamily)