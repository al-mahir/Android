package com.example.mushaf.presentation.guide

enum class MushafGuideStep(val total: Int = 6) {
    SURAH_NAME,
    AYAH_LONG_PRESS,
    MODE_READING,
    MODE_LISTEN,
    MODE_RECITATION,
    MODE_MUALLEM;

    val number get() = ordinal + 1
}
