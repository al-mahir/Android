package com.example.mushaf.domain.model


enum class ReadingMode {
    TAJWEED,

    PLAIN;

    companion object {
        fun from(tajweedEnabled: Boolean): ReadingMode = if (tajweedEnabled) TAJWEED else PLAIN
    }
}
