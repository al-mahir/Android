package com.example.mushaf.presentation.highlight

import com.example.mushaf.domain.model.MushafPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class SimulatedHighlightDriver(
    private val scope: CoroutineScope,
    private val stepMillis: Long = 500L,
) : HighlightDriver {

    private val _currentWordId = MutableStateFlow<String?>(null)
    override val currentWordId: StateFlow<String?> = _currentWordId

    private var job: Job? = null

    override fun start(page: MushafPage) {
        stop()
        val words = page.lines.flatMap { it.words }
        if (words.isEmpty()) return
        job = scope.launch {
            for (word in words) {
                if (!isActive) break
                _currentWordId.value = word.id
                delay(stepMillis)
            }
            _currentWordId.value = null
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        _currentWordId.value = null
    }
}
