package com.iti.al_mahir.debug

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mushaf.domain.model.recite.RecitationCursor
import com.example.mushaf.domain.model.recite.local.LocalRecitationCursor
import com.example.mushaf.domain.model.recite.local.SpeechRecognitionAvailability
import com.example.mushaf.domain.repository.LocalSpeechRecognizer
import com.example.mushaf.domain.repository.LocalWordCorpusRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Manual, on-device spike harness (Phase 2 of
 * docs/features/06-taahud-local-recitation-tracking-plan.md) — NOT part of the app's real
 * navigation graph, debug builds only. Answers one question: is Android's built-in on-device
 * Arabic recognizer accurate enough on real Quranic recitation to be worth wiring into the live
 * cursor, before committing to a heavier engine.
 *
 * Launch directly, bypassing the app's normal entry point:
 *   adb shell am start -n com.iti.al_mahir/.debug.LocalRecitationSpikeActivity
 *
 * Recite Al-Fātiḥa (or any window below) into the mic and watch whether the recognized word
 * lines up with the highlighted local match. This is exactly the shape of the eventual live
 * pipeline (settled word -> ArabicPhoneticMatcher -> nearest-window match), just wired to a
 * screen for a human to read instead of to MushafPageView.
 */
class LocalRecitationSpikeActivity : ComponentActivity() {

    private val speechRecognizer: LocalSpeechRecognizer by inject()
    private val corpus: LocalWordCorpusRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpikeScreen(speechRecognizer, corpus)
                }
            }
        }
    }
}

private data class WordAttempt(val heard: String, val matchedWordId: String?, val matchedText: String?)

@Composable
private fun SpikeScreen(recognizer: LocalSpeechRecognizer, corpus: LocalWordCorpusRepository) {
    val scope = rememberCoroutineScope()
    var listenJob by remember { mutableStateOf<Job?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var availability by remember { mutableStateOf<SpeechRecognitionAvailability?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val attempts = remember { mutableStateListOf<WordAttempt>() }
    var biasIndex by remember { mutableIntStateOf(0) }
    var window by remember { mutableStateOf(emptyList<com.example.mushaf.domain.model.recite.local.LocalWordEntry>()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) errorText = "RECORD_AUDIO denied"
    }

    fun loadWindow(sura: Int, aya: Int) {
        scope.launch {
            window = corpus.wordsFrom(RecitationCursor(sura, aya, 0), count = 15)
            biasIndex = 0
            attempts.clear()
        }
    }

    fun start() {
        errorText = null
        availability = recognizer.availability()
        if (availability == SpeechRecognitionAvailability.UNAVAILABLE) {
            errorText = "No speech recognizer on this device"
            return
        }
        isListening = true
        listenJob = scope.launch {
            runCatching {
                recognizer.listen().collect { heard ->
                    val matchIndex = LocalRecitationCursor.resolve(heard, window, biasIndex)
                    val matched = matchIndex?.let { window[it] }
                    if (matchIndex != null) biasIndex = matchIndex
                    attempts.add(0, WordAttempt(heard, matched?.wordId, matched?.plainText))
                    Log.d(
                        "LocalRecitationSpike",
                        "heard='$heard' -> ${matched?.wordId ?: "NO MATCH"} (${matched?.plainText})",
                    )
                }
            }.onFailure { errorText = it.message; isListening = false }
        }
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null
        isListening = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Local recitation spike (debug only)", style = MaterialTheme.typography.titleMedium)
        Text("availability: ${availability ?: "unknown - press Start"}")
        errorText?.let { Text("error: $it") }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Grant mic")
            }
            Button(onClick = { loadWindow(1, 1) }) { Text("Load Al-Fātiḥa 1:1") }
            Button(onClick = { loadWindow(2, 1) }) { Text("Load Al-Baqarah 2:1") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            Button(onClick = ::start, enabled = !isListening && window.isNotEmpty()) { Text("Start") }
            Button(onClick = ::stop, enabled = isListening) { Text("Stop") }
        }

        Text("Window (bias index=$biasIndex):")
        Text(window.joinToString(" | ") { "${it.wordId}:${it.plainText}" })

        Text("Attempts (newest first):", modifier = Modifier.padding(top = 12.dp))
        LazyColumn {
            items(attempts) { attempt ->
                Text("heard='${attempt.heard}' -> ${attempt.matchedWordId ?: "NO MATCH"} (${attempt.matchedText ?: "-"})")
            }
        }
    }
}
