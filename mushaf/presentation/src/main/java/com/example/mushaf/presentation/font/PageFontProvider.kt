package com.example.mushaf.presentation.font

import android.content.Context
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.mushaf.domain.model.ReadingMode


object PageFontProvider {

    private const val TAG = "Mushaf"


    private const val FONT_CACHE_SIZE = 24
    private val fontCache = LruCache<String, FontFamily>(FONT_CACHE_SIZE)

    const val SURAH_NAME_FONT_PATH = "fonts/ornament/surah_names.ttf"

    const val SURAH_NAME_PUA_BASE = 0xE900


    val SURAH_NAME_CODEPOINTS: IntArray = intArrayOf(
        0xE904, 0xE905, 0xE906, 0xE907, 0xE908, 0xE90B, 0xE90C, 0xE90D, 0xE90E, 0xE90F,
        0xE910, 0xE911, 0xE912, 0xE913, 0xE914, 0xE915, 0xE916, 0xE917, 0xE918, 0xE919,
        0xE91A, 0xE91B, 0xE91C, 0xE91D, 0xE91E, 0xE91F, 0xE920, 0xE921, 0xE922, 0xE923,
        0xE924, 0xE925, 0xE926, 0xE92E, 0xE92F, 0xE930, 0xE931, 0xE909, 0xE90A, 0xE927,
        0xE928, 0xE929, 0xE92A, 0xE92B, 0xE92C, 0xE92D, 0xE932, 0xE902, 0xE933, 0xE934,
        0xE935, 0xE936, 0xE937, 0xE938, 0xE939, 0xE93A, 0xE93B, 0xE93C, 0xE900, 0xE901,
        0xE941, 0xE942, 0xE943, 0xE944, 0xE945, 0xE946, 0xE947, 0xE948, 0xE949, 0xE94A,
        0xE94B, 0xE94C, 0xE94D, 0xE94E, 0xE94F, 0xE950, 0xE951, 0xE952, 0xE93D, 0xE93E,
        0xE93F, 0xE940, 0xE953, 0xE954, 0xE955, 0xE956, 0xE957, 0xE958, 0xE959, 0xE95A,
        0xE95B, 0xE95C, 0xE95D, 0xE95E, 0xE95F, 0xE960, 0xE961, 0xE962, 0xE963, 0xE964,
        0xE965, 0xE966, 0xE967, 0xE968, 0xE969, 0xE96A, 0xE96B, 0xE96C, 0xE96D, 0xE96E,
        0xE96F, 0xE970, 0xE971, 0xE972,
    )

    fun assetPath(pageNumber: Int, mode: ReadingMode): String {
        val dir = if (mode == ReadingMode.TAJWEED) "tajweed" else "standard"
        return "fonts/$dir/p$pageNumber.ttf"
    }

    fun surahNameGlyph(surahNumber: Int): String? {
        val cp = SURAH_NAME_CODEPOINTS.getOrNull(surahNumber - 1) ?: return null
        return String(Character.toChars(cp))
    }

    fun createFromAsset(context: Context, path: String): FontFamily? =
        if (assetExists(context, path)) {
            FontFamily(Font(path = path, assetManager = context.assets))
        } else {
            Log.w(TAG, "Ornamental font asset missing: $path")
            null
        }


    fun create(context: Context, pageNumber: Int, mode: ReadingMode): FontFamily {
        val path = assetPath(pageNumber, mode)
        fontCache.get(path)?.let { return it }
        val resolved = resolve(context, pageNumber, mode, path)
        fontCache.put(path, resolved)
        return resolved
    }

    private fun resolve(context: Context, pageNumber: Int, mode: ReadingMode, path: String): FontFamily {
        if (assetExists(context, path)) {
            return FontFamily(Font(path = path, assetManager = context.assets))
        }
        Log.w(TAG, "Font asset missing: $path — attempting fallback")
        val fallbackMode = if (mode == ReadingMode.TAJWEED) ReadingMode.PLAIN else ReadingMode.TAJWEED
        val fallbackPath = assetPath(pageNumber, fallbackMode)
        if (assetExists(context, fallbackPath)) {
            return FontFamily(Font(path = fallbackPath, assetManager = context.assets))
        }
        Log.e(TAG, "No font asset for page $pageNumber ($path / $fallbackPath); using default font")
        return FontFamily.Default
    }

    private fun assetExists(context: Context, path: String): Boolean = try {
        context.assets.open(path).use { true }
    } catch (t: Throwable) {
        false
    }
}


@Composable
fun rememberPageFontFamily(pageNumber: Int, mode: ReadingMode): FontFamily {
    val context = LocalContext.current
    val path = PageFontProvider.assetPath(pageNumber, mode)
    return remember(path) { PageFontProvider.create(context, pageNumber, mode) }
}

@Composable
fun rememberSurahNameFontFamily(): FontFamily? {
    val context = LocalContext.current
    return remember { PageFontProvider.createFromAsset(context, PageFontProvider.SURAH_NAME_FONT_PATH) }
}
