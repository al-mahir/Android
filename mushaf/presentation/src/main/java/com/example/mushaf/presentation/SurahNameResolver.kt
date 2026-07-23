package com.example.mushaf.presentation

import com.iti.domain.model.quran.SurahNames






 
internal object SurahNameResolver {

    fun nameFor(surahNumber: Int?): String =
        SurahNames.nameOf(surahNumber)?.let { "سُورَةُ $it" } ?: ""

     
    fun plainNameFor(surahNumber: Int?): String = SurahNames.nameOf(surahNumber).orEmpty()
}
