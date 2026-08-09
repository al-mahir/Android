package com.example.mushaf.data.recite.local.asr

import android.util.Log
import com.example.mushaf.data.MushafLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reads `ordered_quran_phonemes.json` - 6236 ayahs of reference pronunciation, downloaded next to
 * the model weights by [AsrModelRepositoryImpl] - into memory once, on first use.
 *
 * Parsed eagerly and kept resident rather than queried per ayah: the file is a single JSON object,
 * so there is no way to read one ayah out of it without walking the whole thing anyway, and the
 * live path needs a fresh window every couple of seconds. Only `aya_phonemes_list` is retained;
 * the aya text and the flat phoneme string alongside it are never used here and would roughly
 * triple the footprint.
 */
class QuranPhonemeReferenceSource(
    private val asrModelRepository: AsrModelRepositoryImpl,
) {

    @Serializable
    private class AyahPhonemesDto(
        @SerialName("aya_phonemes_list") val units: List<String> = emptyList(),
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val loadMutex = Mutex()

    @Volatile
    private var unitsByAyah: Map<String, List<String>>? = null

    @Volatile
    private var loadFailed = false

    /**
     * The pronunciation units of one ayah in reading order, or `null` when the table isn't
     * available (still downloading, download failed, or the ayah is missing from it). Never
     * throws - the caller's fallback is simply "no local prediction here".
     */
    suspend fun unitsFor(sura: Int, aya: Int): List<String>? = load()?.get("$sura:$aya")

    private suspend fun load(): Map<String, List<String>>? {
        unitsByAyah?.let { return it }
        if (loadFailed) return null
        return loadMutex.withLock {
            unitsByAyah?.let { return@withLock it }
            if (loadFailed) return@withLock null
            val file = asrModelRepository.phonemeReferenceFileOrNull() ?: return@withLock null
            withContext(Dispatchers.IO) {
                runCatching {
                    json.decodeFromString<Map<String, AyahPhonemesDto>>(file.readText())
                        .mapValues { (_, dto) -> dto.units }
                }
            }.onFailure { error ->
                // A corrupt or truncated table must not keep costing a 5MB parse per window.
                loadFailed = true
                Log.w(MushafLog.TAG, "Phoneme reference table could not be read", error)
            }.getOrNull()?.also {
                Log.d(MushafLog.TAG, "Phoneme reference table loaded: ${it.size} ayahs")
                unitsByAyah = it
            }
        }
    }
}
