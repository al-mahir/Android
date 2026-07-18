package com.example.mushaf.domain.model

/**
 * The kind of printed line on a Mushaf page, mirroring the `line_type` column of the
 * QPC v4 layout database.
 */
enum class LineType {
    AYAH,

    BASMALLAH,

    SURAH_NAME,
}
