package com.example.mushaf.presentation.search.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

fun String.highlight(query: String, color: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(this)
    val builder = AnnotatedString.Builder(this)
    var startIndex = this.indexOf(query, ignoreCase = true)
    while (startIndex >= 0) {
        builder.addStyle(
            style = SpanStyle(color = color),
            start = startIndex,
            end = startIndex + query.length
        )
        startIndex = this.indexOf(query, startIndex + query.length, ignoreCase = true)
    }
    return builder.toAnnotatedString()
}
