package com.example.mushaf.presentation.highlight

import com.example.mushaf.domain.model.MushafPage
import kotlinx.coroutines.flow.Flow


interface HighlightDriver {
    val currentWordId: Flow<String?>

    fun start(page: MushafPage)

    fun stop()
}
